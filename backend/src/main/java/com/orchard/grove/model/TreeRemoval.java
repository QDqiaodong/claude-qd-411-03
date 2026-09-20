package com.orchard.grove.model;

public class TreeRemoval {
    public Long id;
    public Long treeId;
    public String reason;
    public String prevStatus;
    public String status; // 有效 / 已撤回
    public String createdAt;
    public String withdrawnAt;
}
