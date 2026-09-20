package com.orchard.grove.mapper;

import com.orchard.grove.model.HarvestBatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface HarvestBatchMapper {
    List<HarvestBatch> findAll();
    HarvestBatch findById(@Param("id") Long id);
    /** 行锁读取：同一批次的调账（改品种/改实际公斤）在此排队，后一笔必须等先一笔提交后看到最新账 */
    HarvestBatch findByIdForUpdate(@Param("id") Long id);
    HarvestBatch findByPlotAndDate(@Param("plotId") Long plotId, @Param("batchDate") String batchDate);
    int insert(HarvestBatch b);
    int update(HarvestBatch b);
    /** 已入仓批次调账用：version 匹配才落库并自增，返回 0 说明账已被别人先动过，整笔回滚报错 */
    int casUpdate(@Param("b") HarvestBatch b, @Param("version") Long version);
    /** 该树挂着的未入仓批次（待采/采集中），清树前据此拦截 */
    List<HarvestBatch> findUnstoredByTree(@Param("treeId") Long treeId);
    List<Long> findTreeIds(@Param("batchId") Long batchId);
    int insertBatchTree(@Param("batchId") Long batchId, @Param("treeId") Long treeId);
    int deleteBatchTrees(@Param("batchId") Long batchId);
}
