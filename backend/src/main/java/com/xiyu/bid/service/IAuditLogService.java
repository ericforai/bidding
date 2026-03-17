package com.xiyu.bid.service;

import com.xiyu.bid.audit.dto.AuditLogQueryResponse;
import com.xiyu.bid.entity.AuditLog;

import java.time.LocalDateTime;

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

    AuditLog logSync(AuditLogService.AuditLogEntry entry);

    AuditLogQueryResponse queryLogs(String keyword,
                                    String action,
                                    String module,
                                    String operator,
                                    LocalDateTime start,
                                    LocalDateTime end,
                                    Boolean success);
}
