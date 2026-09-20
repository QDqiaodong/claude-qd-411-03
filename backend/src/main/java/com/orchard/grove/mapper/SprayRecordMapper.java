package com.orchard.grove.mapper;

import com.orchard.grove.model.SprayRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SprayRecordMapper {
    List<SprayRecord> findAll();
    List<SprayRecord> findByPlot(@Param("plotId") Long plotId);
    SprayRecord findById(@Param("id") Long id);
    int insert(SprayRecord s);
    int voidById(@Param("id") Long id);
}
