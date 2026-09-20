package com.orchard.grove.service;

import com.orchard.grove.dto.BizException;
import com.orchard.grove.mapper.HarvestBatchMapper;
import com.orchard.grove.mapper.PlotMapper;
import com.orchard.grove.mapper.TreeMapper;
import com.orchard.grove.mapper.TreeRemovalMapper;
import com.orchard.grove.model.HarvestBatch;
import com.orchard.grove.model.Plot;
import com.orchard.grove.model.Tree;
import com.orchard.grove.model.TreeRemoval;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TreeService {
    @Autowired
    TreeMapper treeMapper;
    @Autowired
    PlotMapper plotMapper;
    @Autowired
    TreeRemovalMapper removalMapper;
    @Autowired
    HarvestBatchMapper batchMapper;

    public List<Tree> list() {
        List<Tree> all = treeMapper.findAll();
        Map<Long, TreeRemoval> active = new HashMap<>();
        for (TreeRemoval r : removalMapper.findActive()) active.put(r.treeId, r);
        for (Tree t : all) {
            TreeRemoval r = active.get(t.id);
            if (r != null) {
                t.removalId = r.id;
                t.removalReason = r.reason;
            }
        }
        return all;
    }

    public Tree create(Tree f) {
        if (f.code == null || f.code.isBlank()) throw new BizException("果树编号必填");
        if (treeMapper.findByCode(f.code) != null) throw new BizException("果树编号 " + f.code + " 已存在");
        if (f.plotId == null) throw new BizException("必须指定归属地块");
        Plot plot = plotMapper.findById(f.plotId);
        if (plot == null) throw new BizException("归属地块不存在");
        if (!"在用".equals(plot.status)) throw new BizException("只能归到在用地块");
        if (f.variety == null || f.variety.isBlank()) throw new BizException("品种必填");
        Tree t = new Tree();
        t.code = f.code;
        t.plotId = f.plotId;
        t.variety = f.variety;
        t.plantYear = f.plantYear;
        t.status = (f.status == null || f.status.isBlank()) ? "正常" : f.status;
        t.note = f.note;
        treeMapper.insert(t);
        return t;
    }

    public Tree update(Long id, Tree f) {
        Tree t = treeMapper.findById(id);
        if (t == null) throw new BizException("果树不存在");
        if (f.code != null && !f.code.isBlank()) t.code = f.code;
        if (f.plotId != null) {
            Plot plot = plotMapper.findById(f.plotId);
            if (plot == null) throw new BizException("归属地块不存在");
            if (!"在用".equals(plot.status)) throw new BizException("只能归到在用地块");
            t.plotId = f.plotId;
        }
        if (f.variety != null && !f.variety.isBlank()) t.variety = f.variety;
        if (f.plantYear != null) t.plantYear = f.plantYear;
        if (f.status != null && !f.status.isBlank() && !f.status.equals(t.status)) {
            // 「已清」必须经清树单接口进出，否则株数、批次、清树单三处台账会对不上
            if ("已清".equals(t.status) || "已清".equals(f.status))
                throw new BizException("「已清」状态只能通过清树 / 撤回清树单变更");
            t.status = f.status;
        }
        if (f.note != null) t.note = f.note;
        treeMapper.update(t);
        return t;
    }

    /**
     * 清树。先锁树行再做 CAS 置「已清」，同一棵树只许留一笔有效清树单（并发下只有一笔能改成功）；
     * 挂着待采/采集中批次的树必须挡住并报出批次（检查在持锁后做，与开批次选树互斥）。
     * 只动树与清树单，已入仓的库存公斤数不跟着清树加减。
     */
    @Transactional
    public TreeRemoval clear(Long id, TreeRemoval f) {
        Tree t = treeMapper.findByIdForUpdate(id);
        if (t == null) throw new BizException("果树不存在");
        if (treeMapper.clearIfNotCleared(id) != 1) {
            TreeRemoval active = removalMapper.findActiveByTree(id);
            throw new BizException("清树未生效：果树 " + t.code + " 已有一笔有效清树单"
                    + (active == null ? "" : "（#" + active.id
                        + (active.reason == null ? "" : "，原因：" + active.reason)
                        + "，登记于 " + active.createdAt + "）")
                    + "，同一棵树只保留一笔");
        }
        List<HarvestBatch> open = batchMapper.findUnstoredByTree(id);
        if (!open.isEmpty()) {
            String detail = open.stream().map(b -> {
                Plot p = plotMapper.findById(b.plotId);
                String plotName = p == null ? "" : p.code + (p.name == null ? "" : " " + p.name);
                return "#" + b.id + " " + plotName + " · " + b.batchDate + "（" + b.status + "）";
            }).collect(Collectors.joining("；"));
            // 抛错回滚，上面的状态变更一并回滚，树保持未清
            throw new BizException("清树被拦截：果树 " + t.code + " 还挂在未入仓批次 " + detail
                    + "，请先把该树从批次摘掉或等批次入仓再清树");
        }
        TreeRemoval r = new TreeRemoval();
        r.treeId = id;
        r.reason = (f != null && f.reason != null && !f.reason.isBlank()) ? f.reason.trim() : "病弱清树";
        r.prevStatus = t.status;
        r.status = "有效";
        r.createdAt = now();
        removalMapper.insert(r);
        return r;
    }

    /**
     * 撤回清树单：株数加回去（树恢复清树前状态）。撤回只动清树单与树本身——
     * 撤回前已从某张未入仓批次里摘掉的树不自动加回，不把那条批次改回可推；
     * 入仓库存同样不跟着撤回加减。
     */
    @Transactional
    public TreeRemoval withdraw(Long id) {
        Tree t = treeMapper.findById(id);
        if (t == null) throw new BizException("果树不存在");
        TreeRemoval active = removalMapper.findActiveByTree(id);
        if (active == null) throw new BizException("果树 " + t.code + " 没有可撤回的有效清树单");
        if (removalMapper.withdrawById(active.id, now()) != 1)
            throw new BizException("该清树单已被撤回，请刷新后重试");
        String prev = (active.prevStatus == null || active.prevStatus.isBlank() || "已清".equals(active.prevStatus))
                ? "正常" : active.prevStatus;
        if (treeMapper.restoreIfCleared(id, prev) != 1)
            throw new BizException("果树状态已变化，撤回失败，请刷新后重试");
        active.status = "已撤回";
        active.withdrawnAt = now();
        return active;
    }

    private String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
