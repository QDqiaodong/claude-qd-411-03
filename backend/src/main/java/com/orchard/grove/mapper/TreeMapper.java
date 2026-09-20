package com.orchard.grove.mapper;

import com.orchard.grove.model.Tree;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TreeMapper {
    List<Tree> findAll();
    Tree findById(@Param("id") Long id);
    /** 行锁读取：清树与开批次选树互斥，避免「已清树挂进新批次 / 挂批次的树被清掉」的并发漏判 */
    Tree findByIdForUpdate(@Param("id") Long id);
    Tree findByCode(@Param("code") String code);
    int countByPlotAndStatus(@Param("plotId") Long plotId, @Param("status") String status);
    int insert(Tree t);
    int update(Tree t);
    /** 清树 CAS：仅当树未清时置为「已清」，返回 0 说明已有有效清树单（并发/重复） */
    int clearIfNotCleared(@Param("id") Long id);
    /** 撤回 CAS：仅当树处于「已清」时恢复为清树前状态 */
    int restoreIfCleared(@Param("id") Long id, @Param("status") String status);
}
