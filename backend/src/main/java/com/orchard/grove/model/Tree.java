package com.orchard.grove.model;

public class Tree {
    public Long id;
    public String code;
    public Long plotId;
    public String variety;
    public Integer plantYear;
    public String status;
    public String note;
    // 计算字段（不入库）：当前有效清树单，供果树卡片展示「已清」依据
    public Long removalId;
    public String removalReason;
}
