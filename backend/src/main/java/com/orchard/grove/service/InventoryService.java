package com.orchard.grove.service;

import com.orchard.grove.dto.BizException;
import com.orchard.grove.mapper.InventoryMapper;
import com.orchard.grove.model.Inventory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.TreeMap;

@Service
public class InventoryService {
    @Autowired
    InventoryMapper inventoryMapper;

    public List<Inventory> list() {
        return inventoryMapper.findAll();
    }

    public void onStored(String variety, Double kg) {
        if (variety == null || variety.isBlank()) throw new BizException("品种必填");
        inventoryMapper.upsert(variety, 0.0);
        inventoryMapper.addStock(variety, kg);
    }

    /**
     * 已入仓批次事后调账：把这一笔入仓公斤在库存上对齐到最新登记。
     * - 只改品种：旧品种扣回本笔入仓公斤（按当时实际），新品种加同一笔，等于库存划转；
     * - 只改实际公斤：库存只跟新旧实际的差额走（调大补差、调小扣回）；
     * - 品种和公斤一起改：按旧品种扣旧实际、新品种加新实际，整体仍是一笔调账。
     * 涉及到的品种行按品种名排序取行锁（与出库、其它批次调账共用同一顺序），避免两笔跨品种划转互锁。
     */
    public void reconcileStored(String oldVariety, Double oldActual, String newVariety, Double newActual) {
        if (oldVariety == null || oldVariety.isBlank() || newVariety == null || newVariety.isBlank())
            throw new BizException("品种必填");
        if (oldActual == null || oldActual < 0 || newActual == null || newActual < 0)
            throw new BizException("实际公斤不合法");
        boolean varietyChanged = !oldVariety.equals(newVariety);
        boolean kgChanged = Double.compare(oldActual, newActual) != 0;
        if (!varietyChanged && !kgChanged) return;

        // 每个品种的净增减（同一品种在两边各出现一次时轧差），再按品种名排序落库，保证全局加锁顺序一致
        TreeMap<String, Double> delta = new TreeMap<>();
        if (varietyChanged) {
            delta.merge(oldVariety, -oldActual, Double::sum);
            delta.merge(newVariety, newActual, Double::sum);
        } else {
            delta.merge(newVariety, newActual - oldActual, Double::sum);
        }
        for (var e : delta.entrySet()) {
            if (Double.compare(e.getValue(), 0.0) == 0) continue;
            inventoryMapper.upsert(e.getKey(), 0.0);
            if (e.getValue() > 0) inventoryMapper.addStock(e.getKey(), e.getValue());
            else inventoryMapper.subtractStock(e.getKey(), -e.getValue());
        }
    }

    public Inventory outbound(String variety, Double kg) {
        Inventory inv = inventoryMapper.findByVariety(variety);
        if (inv == null) throw new BizException("该品种无库存记录");
        if (inv.stockKg == null || inv.stockKg < kg)
            throw new BizException("库存不足，无法出库（现有 " + (inv.stockKg == null ? 0 : inv.stockKg) + "kg）");
        inventoryMapper.subtractStock(variety, kg);
        return inventoryMapper.findByVariety(variety);
    }

    public Inventory setWarnLine(String variety, Double warnLine) {
        Inventory inv = inventoryMapper.findByVariety(variety);
        if (inv == null) throw new BizException("该品种无库存记录");
        inv.warnLine = warnLine;
        inventoryMapper.update(inv);
        return inv;
    }
}
