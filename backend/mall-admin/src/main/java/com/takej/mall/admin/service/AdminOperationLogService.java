package com.takej.mall.admin.service;

import com.takej.mall.admin.entity.AdminOperationLog;
import com.takej.mall.admin.mapper.AdminOperationLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;


/**
 * 后台管理员操作日志服务。
 * 只负责追加记录，不提供修改和删除。
 */
@Service
public class AdminOperationLogService {

    private static final Logger log =
            LoggerFactory.getLogger(AdminOperationLogService.class);

    private final AdminOperationLogMapper adminOperationLogMapper;

    public AdminOperationLogService(
            AdminOperationLogMapper adminOperationLogMapper) {

        this.adminOperationLogMapper = adminOperationLogMapper;
    }

    /**
     * 记录一条管理员操作日志。
     * 记录失败只写服务日志，不抛出异常，避免影响已经完成的主业务。
     *
     * @param adminId    操作管理员 ID
     * @param operation  操作类型，取值见 AdminOperationConstants
     * @param targetType 目标对象类型
     * @param targetId   目标对象 ID
     * @param detail     操作摘要
     */
    public void record(
            Long adminId,
            String operation,
            String targetType,
            Long targetId,
            String detail) {

        try {
            AdminOperationLog operationLog =
                    new AdminOperationLog();

            operationLog.setAdminId(adminId);
            operationLog.setOperation(operation);
            operationLog.setTargetType(targetType);
            operationLog.setTargetId(targetId);
            operationLog.setDetail(detail);

            adminOperationLogMapper.insert(operationLog);

        } catch (RuntimeException exception) {
            // 日志失败不能反过来影响主业务，这里只留下错误线索。
            log.error(
                    "记录管理员操作日志失败，adminId={}, operation={}, targetType={}, targetId={}",
                    adminId,
                    operation,
                    targetType,
                    targetId,
                    exception
            );
        }
    }


    /**
     * 查询最近的管理员操作日志，最新在前。
     * 固定取最近 100 条：日志表只增不减，不能无条件全量拉取。
     */
    public List<AdminOperationLog> listRecentLogs() {
        LambdaQueryWrapper<AdminOperationLog> query =
                new LambdaQueryWrapper<>();

        query.orderByDesc(AdminOperationLog::getId)
                .last("LIMIT 100");

        return adminOperationLogMapper.selectList(query);
    }


}