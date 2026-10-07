package com.takej.mall.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 后台管理员操作日志，对应 admin_operation_log 表。
 * 只追加、不修改、不删除，用于追溯管理员做过什么。
 */
@TableName("admin_operation_log")
public class AdminOperationLog {

    /** 日志主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作管理员 ID。 */
    private Long adminId;

    /** 操作类型，例如 CREATE_PRODUCT、ADJUST_STOCK。 */
    private String operation;

    /** 目标对象类型，例如 PRODUCT、ORDER。 */
    private String targetType;

    /** 目标对象 ID。 */
    private Long targetId;

    /** 操作摘要。 */
    private String detail;

    /** 操作时间。 */
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAdminId() {
        return adminId;
    }

    public void setAdminId(Long adminId) {
        this.adminId = adminId;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}