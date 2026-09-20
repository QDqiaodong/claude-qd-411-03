package com.orchard.grove.service;

import com.orchard.grove.dto.BizException;
import com.orchard.grove.mapper.PlotMapper;
import com.orchard.grove.model.Plot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlotService {
    @Autowired
    PlotMapper plotMapper;

    public List<Plot> list() {
        List<Plot> all = plotMapper.findAll();
        for (Plot p : all) p.productiveTrees = plotMapper.countTrees(p.id);
        return all;
    }

    public Plot create(Plot f) {
        if (f.code == null || f.code.isBlank()) throw new BizException("地块编号必填");
        if (plotMapper.findByCode(f.code) != null) throw new BizException("地块编号 " + f.code + " 已存在");
        if (f.area == null || f.area <= 0) throw new BizException("面积必须大于 0");
        Plot p = new Plot();
        p.code = f.code;
        p.name = f.name;
        p.area = f.area;
        p.status = (f.status == null || f.status.isBlank()) ? "在用" : f.status;
        p.note = f.note;
        plotMapper.insert(p);
        return p;
    }

    public Plot update(Long id, Plot f) {
        Plot p = plotMapper.findById(id);
        if (p == null) throw new BizException("地块不存在");
        if (f.code != null && !f.code.isBlank()) p.code = f.code;
        if (f.name != null) p.name = f.name;
        if (f.area != null) {
            if (f.area <= 0) throw new BizException("面积必须大于 0");
            p.area = f.area;
        }
        if (f.status != null && !f.status.isBlank()) {
            if ("停用".equals(f.status) && plotMapper.countTrees(id) > 0)
                throw new BizException("该地块还有在管果树，不能停用");
            p.status = f.status;
        }
        if (f.note != null) p.note = f.note;
        plotMapper.update(p);
        return p;
    }
}
