package com.orchard.grove.service;

import com.orchard.grove.dto.BizException;
import com.orchard.grove.mapper.HarvestBatchMapper;
import com.orchard.grove.mapper.PlotMapper;
import com.orchard.grove.mapper.TreeMapper;
import com.orchard.grove.model.HarvestBatch;
import com.orchard.grove.model.Plot;
import com.orchard.grove.model.SprayRecord;
import com.orchard.grove.model.Tree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class HarvestBatchService {
    @Autowired
    HarvestBatchMapper batchMapper;
    @Autowired
    PlotMapper plotMapper;
    @Autowired
    TreeMapper treeMapper;
    @Autowired
    InventoryService inventoryService;
    @Autowired
    SprayService sprayService;

    public List<HarvestBatch> list() {
        List<HarvestBatch> all = batchMapper.findAll();
        for (HarvestBatch b : all) {
            b.treeIds = batchMapper.findTreeIds(b.id);
            b.blocked = false;
            if (!"已入仓".equals(b.status)) {
                SprayRecord s = sprayService.blockingSpray(b.plotId, b.batchDate);
                if (s != null) {
                    b.blocked = true;
                    b.blockReason = sprayService.blockReason(s) + "，本批次在解禁前不能推进";
                }
            }
        }
        return all;
    }

    @Transactional
    public HarvestBatch create(HarvestBatch f) {
        if (f.plotId == null) throw new BizException("必须指定地块");
        Plot plot = plotMapper.findById(f.plotId);
        if (plot == null) throw new BizException("地块不存在");
        if (!"在用".equals(plot.status)) throw new BizException("停用地块不能开采摘批次");
        SprayRecord block = sprayService.plotIntervalBlock(f.plotId);
        if (block != null)
            throw new BizException(sprayService.blockReason(block) + "，间隔未满前不能新开采摘批次");
        if (f.batchDate == null || f.batchDate.isBlank()) throw new BizException("采摘日期必填");
        if (batchMapper.findByPlotAndDate(f.plotId, f.batchDate) != null)
            throw new BizException("该地块 " + f.batchDate + " 已有一批采摘");
        if (f.variety == null || f.variety.isBlank()) throw new BizException("品种必填");
        if (f.estimateKg == null || f.estimateKg <= 0) throw new BizException("预估产量必须大于 0");
        List<Long> treeIds = checkTrees(f.plotId, f.treeIds);
        HarvestBatch b = new HarvestBatch();
        b.plotId = f.plotId;
        b.batchDate = f.batchDate;
        b.variety = f.variety;
        b.estimateKg = f.estimateKg;
        b.status = "待采";
        b.actualKg = null;
        batchMapper.insert(b);
        saveTrees(b.id, treeIds);
        b.treeIds = batchMapper.findTreeIds(b.id);
        return b;
    }

    @Transactional
    public HarvestBatch update(Long id, HarvestBatch f) {
        // 行锁读批次：同一笔批次上叠着来的两笔修改在此排队，后一笔拿到的一定是先一笔提交后的最新账
        HarvestBatch b = batchMapper.findByIdForUpdate(id);
        if (b == null) throw new BizException("采摘批次不存在");
        if ("已入仓".equals(b.status)) {
            updateStored(b, f);
        } else {
            updateActive(b, f);
        }
        b.treeIds = batchMapper.findTreeIds(b.id);
        return b;
    }

    /**
     * 已入仓批次只能调品种和实际公斤，且必须带着入仓时看到的 version。
     * 库存随调账一起在本事务内改：先把不合法的请求全部拦在库存动作之前（实际超预估等），
     * 再做库存划转/差额，最后 CAS 落批次；CAS 失败说明等待行锁期间账又被改过，整笔回滚，
     * 库存一行都不会留下半截加减。
     */
    private void updateStored(HarvestBatch b, HarvestBatch f) {
        if (f.version == null)
            throw new BizException("批次已在别处被改动，请刷新后按最新数据再改");
        if (!f.version.equals(b.version))
            throw new BizException("批次已在别处被改动（最新版本 " + b.version + "），请刷新看到最新库存账后再改");
        if (f.status != null && !f.status.isBlank() && !f.status.equals(b.status))
            throw new BizException("已入仓批次状态不能再改");
        if (f.treeIds != null)
            throw new BizException("已入仓批次的果树名单不能再改");
        if (f.estimateKg != null && Double.compare(f.estimateKg, b.estimateKg) != 0)
            throw new BizException("已入仓批次的预估产量（入仓时上限 " + b.estimateKg + "kg）不能再改");

        String newVariety = (f.variety != null && !f.variety.isBlank()) ? f.variety : b.variety;
        Double newActual = f.actualKg != null ? f.actualKg : b.actualKg;
        if (newActual == null) throw new BizException("实际产量必须登记");
        if (newActual <= 0) throw new BizException("实际产量必须大于 0");
        // 调大不能超过入仓当时的预估
        if (newActual > b.estimateKg)
            throw new BizException("实际产量不得超过入仓时预估 " + b.estimateKg + "kg，库存不动");

        boolean varietyChanged = !newVariety.equals(b.variety);
        boolean kgChanged = Double.compare(newActual, b.actualKg) != 0;
        if (varietyChanged || kgChanged) {
            // 校验全部通过后才碰库存；旧品种扣回这一笔、新品种加上同一笔（改公斤时只走差额）
            inventoryService.reconcileStored(b.variety, b.actualKg, newVariety, newActual);
            b.variety = newVariety;
            b.actualKg = newActual;
        }
        int rows = batchMapper.casUpdate(b, f.version);
        if (rows == 0)
            throw new BizException("批次已在别处被改动，请刷新看到最新库存账后再改，本笔未改动库存");
        b.version = f.version + 1;
    }

    private void updateActive(HarvestBatch b, HarvestBatch f) {
        if (f.status != null && !f.status.isBlank() && !f.status.equals(b.status)) {
            String next = f.status;
            boolean ok = ("待采".equals(b.status) && "采集中".equals(next))
                    || ("采集中".equals(b.status) && "已入仓".equals(next))
                    || ("待采".equals(b.status) && "已入仓".equals(next));
            if (!ok) throw new BizException("采摘状态只能 待采→采集中→已入仓");
            SprayRecord block = sprayService.blockingSpray(b.plotId, b.batchDate);
            if (block != null)
                throw new BizException(sprayService.blockReason(block)
                        + "，且施药日不晚于本批次采摘日 " + b.batchDate + "，本批次暂不能推进到「" + next + "」");
            if ("已入仓".equals(next)) {
                Double actual = f.actualKg != null ? f.actualKg : b.actualKg;
                // 超预估被拦：下面直接抛错，本方法此前未动过库存，事务回滚，库存一行都不动
                if (actual == null) throw new BizException("入仓必须登记实际产量");
                if (actual <= 0) throw new BizException("实际产量必须大于 0");
                if (actual > b.estimateKg) throw new BizException("实际产量不得超过预估 " + b.estimateKg + "kg，库存不动");
                inventoryService.onStored(b.variety, actual);
                b.actualKg = actual;
            }
            b.status = next;
        }
        if (f.treeIds != null) {
            // 本请求刚把批次推进到已入仓，则名单也即刻冻结
            if ("已入仓".equals(b.status)) throw new BizException("已入仓批次的果树名单不能再改");
            // 已入仓批次的果树名单是历史事实，冻结；未入仓批次可摘掉/调换果树
            List<Long> treeIds = checkTrees(b.plotId, f.treeIds);
            batchMapper.deleteBatchTrees(b.id);
            saveTrees(b.id, treeIds);
        }
        if (f.estimateKg != null) b.estimateKg = f.estimateKg;
        if (f.variety != null && !f.variety.isBlank()) b.variety = f.variety;
        if (f.actualKg != null && !"已入仓".equals(b.status)) b.actualKg = f.actualKg;
        batchMapper.update(b);
    }

    /**
     * 选入批次的树必须存在、属于本地块、且未清（已清的树不能再进新批次）。
     * 行锁读取（按 id 升序，防死锁）：与清树互斥，校验过的树在本事务提交前不会被清掉。
     */
    private List<Long> checkTrees(Long plotId, List<Long> treeIds) {
        if (treeIds == null) return List.of();
        List<Long> uniq = treeIds.stream().filter(Objects::nonNull).distinct().sorted().toList();
        for (Long tid : uniq) {
            Tree t = treeMapper.findByIdForUpdate(tid);
            if (t == null) throw new BizException("果树 #" + tid + " 不存在");
            if (!plotId.equals(t.plotId)) throw new BizException("果树 " + t.code + " 不属于本地块，不能选入该批次");
            if ("已清".equals(t.status)) throw new BizException("果树 " + t.code + " 已清，不能选入批次");
        }
        return uniq;
    }

    private void saveTrees(Long batchId, List<Long> treeIds) {
        for (Long tid : treeIds) batchMapper.insertBatchTree(batchId, tid);
    }
}
