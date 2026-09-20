package com.orchard.grove.model;

public class Plot {
    public Long id;
    public String code;
    public String name;
    public Double area;
    public String status;
    public String note;
    // 计算字段（不入库）：在产株数 = 未清树的株数，每次现算，保证与清树/撤回对得上账
    public Integer productiveTrees;
}
