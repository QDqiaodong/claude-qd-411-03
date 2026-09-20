package com.orchard.grove.mapper;

import com.orchard.grove.model.TreeRemoval;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TreeRemovalMapper {
    int insert(TreeRemoval r);
    TreeRemoval findActiveByTree(@Param("treeId") Long treeId);
    List<TreeRemoval> findActive();
    int withdrawById(@Param("id") Long id, @Param("withdrawnAt") String withdrawnAt);
}
