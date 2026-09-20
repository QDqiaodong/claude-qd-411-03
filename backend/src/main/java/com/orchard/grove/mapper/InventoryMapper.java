package com.orchard.grove.mapper;

import com.orchard.grove.model.Inventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InventoryMapper {
    List<Inventory> findAll();
    Inventory findByVariety(@Param("variety") String variety);
    int upsert(@Param("variety") String variety, @Param("warnLine") Double warnLine);
    int addStock(@Param("variety") String variety, @Param("kg") Double kg);
    int subtractStock(@Param("variety") String variety, @Param("kg") Double kg);
    int update(Inventory inv);
}
