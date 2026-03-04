package com.xiyu.bid.service;

/**
 * 审计日志服务接口
 * 用于支持单元测试中的 Mock
 */
public interface IAuditLogService {

    /**
     * 记录审计日志
     * @param entry 审计日志条目
     */
    void log(AuditLogService.AuditLogEntry entry);
}
