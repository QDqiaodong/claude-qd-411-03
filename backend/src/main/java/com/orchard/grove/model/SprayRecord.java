package com.orchard.grove.model;

public class SprayRecord {
    public Long id;
    public Long plotId;
    public String pesticide;
    public String sprayDate;
    public Integer intervalDays;
    public String status;
    public String note;
    // 计算字段（不入库）：解禁日 = 施药日 + 间隔天数；间隔未满 = 有效且今天 < 解禁日
    public String safeDate;
    public Boolean inInterval;
}
