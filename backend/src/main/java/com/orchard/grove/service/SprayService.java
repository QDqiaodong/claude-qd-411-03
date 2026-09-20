package com.orchard.grove.service;

import com.orchard.grove.dto.BizException;
import com.orchard.grove.mapper.PlotMapper;
import com.orchard.grove.mapper.SprayRecordMapper;
import com.orchard.grove.model.Plot;
import com.orchard.grove.model.SprayRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class SprayService {
    @Autowired
    SprayRecordMapper sprayMapper;
    @Autowired
    PlotMapper plotMapper;

    public List<SprayRecord> list(Long plotId) {
        List<SprayRecord> all = (plotId == null) ? sprayMapper.findAll() : sprayMapper.findByPlot(plotId);
        all.forEach(this::enrich);
        return all;
    }

    public SprayRecord create(SprayRecord f) {
        if (f.plotId == null) throw new BizException("必须指定地块");
        Plot plot = plotMapper.findById(f.plotId);
        if (plot == null) throw new BizException("地块不存在");
        if (!"在用".equals(plot.status)) throw new BizException("地块已停用，不能再补记施药");
        if (f.pesticide == null || f.pesticide.isBlank()) throw new BizException("药名必填");
        LocalDate sprayDay = parseDate(f.sprayDate, "施药日期");
        if (f.intervalDays == null || f.intervalDays < 0) throw new BizException("安全间隔天数不能为负");
        SprayRecord s = new SprayRecord();
        s.plotId = f.plotId;
        s.pesticide = f.pesticide.trim();
        s.sprayDate = sprayDay.toString();
        s.intervalDays = f.intervalDays;
        s.status = "有效";
        s.note = f.note;
        sprayMapper.insert(s);
        return enrich(s);
    }

    /**
     * 作废药单。作废只改这一张单的状态，批次的放行在每次推进/查询时按剩余有效药单实时重算，
     * 因此不会出现「药单已作废、批次仍被卡」或「药单还在、批次却放行」的对不上账。
     */
    @Transactional
    public SprayRecord voidRecord(Long id) {
        SprayRecord s = sprayMapper.findById(id);
        if (s == null) throw new BizException("药单不存在");
        if ("作废".equals(s.status)) throw new BizException("该药单已作废，不能重复作废");
        if (sprayMapper.voidById(id) != 1) throw new BizException("作废失败，请刷新后重试");
        return enrich(sprayMapper.findById(id));
    }

    /** 地块级拦截：该地块任意一张有效药单间隔未满，则不能新开采摘批次。 */
    public SprayRecord plotIntervalBlock(Long plotId) {
        LocalDate today = LocalDate.now();
        for (SprayRecord s : sprayMapper.findByPlot(plotId)) {
            if (isActive(s) && today.isBefore(safeDate(s))) return enrich(s);
        }
        return null;
    }

    /**
     * 批次级拦截：有效药单间隔未满，且施药日不晚于该批次采摘日（后补药单打回已开批次），
     * 则该未入仓批次不能再往下推。施药日晚于采摘日的药单不影响这条批次。
     */
    public SprayRecord blockingSpray(Long plotId, String batchDate) {
        LocalDate today = LocalDate.now();
        LocalDate harvest = parseDate(batchDate, "采摘日期");
        for (SprayRecord s : sprayMapper.findByPlot(plotId)) {
            if (!isActive(s)) continue;
            LocalDate sprayDay = LocalDate.parse(s.sprayDate);
            if (!sprayDay.isAfter(harvest) && today.isBefore(safeDate(s))) return enrich(s);
        }
        return null;
    }

    public String blockReason(SprayRecord s) {
        return "地块于 " + s.sprayDate + " 施用「" + s.pesticide + "」，安全间隔 " + s.intervalDays
                + " 天，" + s.safeDate + " 才解禁";
    }

    private boolean isActive(SprayRecord s) {
        return "有效".equals(s.status);
    }

    private LocalDate safeDate(SprayRecord s) {
        return LocalDate.parse(s.sprayDate).plusDays(s.intervalDays == null ? 0 : s.intervalDays);
    }

    private SprayRecord enrich(SprayRecord s) {
        s.safeDate = safeDate(s).toString();
        s.inInterval = isActive(s) && LocalDate.now().isBefore(LocalDate.parse(s.safeDate));
        return s;
    }

    private LocalDate parseDate(String v, String label) {
        if (v == null || v.isBlank()) throw new BizException(label + "必填");
        try {
            return LocalDate.parse(v.trim());
        } catch (DateTimeParseException e) {
            throw new BizException(label + "格式应为 yyyy-MM-dd");
        }
    }
}
