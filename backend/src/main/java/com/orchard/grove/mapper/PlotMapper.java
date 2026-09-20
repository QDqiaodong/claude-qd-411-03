package com.orchard.grove.mapper;

import com.orchard.grove.model.Plot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PlotMapper {
    List<Plot> findAll();
    Plot findById(@Param("id") Long id);
    Plot findByCode(@Param("code") String code);
    int countTrees(@Param("plotId") Long plotId);
    int insert(Plot p);
    int update(Plot p);
}
