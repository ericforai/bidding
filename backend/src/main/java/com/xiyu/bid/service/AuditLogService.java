// Input: Repository, 相关依赖
// Output: 业务服务、数据操作
// Pos: Service/业务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.xiyu.bid.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiyu.bid.entity.AuditLog;
import com.xiyu.bid.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 审计日志服务
 * 异步记录系统中的关键操作
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService implements IAuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * 异步记录审计日志
     */
    @Async("auditLogExecutor")
    public void log(AuditLogEntry entry) {
        try {
            AuditLog auditLog = buildAuditLog(entry);
            auditLogRepository.save(auditLog);
            log.debug("Audit log saved: {}", auditLog.getAction());
        } catch (Exception e) {
            log.error("Failed to save audit log", e);
        }
    }

    /**
     * 同步记录审计日志（用于关键操作）
     */
    public AuditLog logSync(AuditLogEntry entry) {
        try {
            AuditLog auditLog = buildAuditLog(entry);
            return auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to save audit log synchronously", e);
            throw new RuntimeException("Failed to create audit log", e);
        }
    }

    /**
     * 记录登录操作
     */
    @Async("auditLogExecutor")
    public void logLogin(String userId, String username, boolean success, String errorMessage) {
        AuditLogEntry entry = AuditLogEntry.builder()
            .userId(userId)
            .username(username)
            .action("LOGIN")
            .description("User " + (success ? "logged in" : "failed to log in"))
            .success(success)
            .errorMessage(errorMessage)
            .build();
        log(entry);
    }

    /**
     * 记录登出操作
     */
    @Async("auditLogExecutor")
    public void logLogout(String userId, String username) {
        AuditLogEntry entry = AuditLogEntry.builder()
            .userId(userId)
            .username(username)
            .action("LOGOUT")
            .description("User logged out")
            .success(true)
            .build();
        log(entry);
    }

    /**
     * 记录创建操作
     */
    @Async("auditLogExecutor")
    public void logCreate(String userId, String username, String entityType, String entityId, Object createdEntity) {
        String newValue = toJsonString(createdEntity);

        AuditLogEntry entry = AuditLogEntry.builder()
            .userId(userId)
            .username(username)
            .action("CREATE")
            .entityType(entityType)
            .entityId(entityId)
            .description("Created " + entityType + ": " + entityId)
            .newValue(newValue)
            .success(true)
            .build();
        log(entry);
    }

    /**
     * 记录更新操作
     */
    @Async("auditLogExecutor")
    public void logUpdate(String userId, String username, String entityType, String entityId,
                          Object oldValue, Object newValue) {
        AuditLogEntry entry = AuditLogEntry.builder()
            .userId(userId)
            .username(username)
            .action("UPDATE")
            .entityType(entityType)
            .entityId(entityId)
            .description("Updated " + entityType + ": " + entityId)
            .oldValue(toJsonString(oldValue))
            .newValue(toJsonString(newValue))
            .success(true)
            .build();
        log(entry);
    }

    /**
     * 记录删除操作
     */
    @Async("auditLogExecutor")
    public void logDelete(String userId, String username, String entityType, String entityId, Object deletedEntity) {
        AuditLogEntry entry = AuditLogEntry.builder()
            .userId(userId)
            .username(username)
            .action("DELETE")
            .entityType(entityType)
            .entityId(entityId)
            .description("Deleted " + entityType + ": " + entityId)
            .oldValue(toJsonString(deletedEntity))
            .success(true)
            .build();
        log(entry);
    }

    /**
     * 记录数据导出操作
     */
    @Async("auditLogExecutor")
    public void logExport(String userId, String username, String entityType, int recordCount) {
        AuditLogEntry entry = AuditLogEntry.builder()
            .userId(userId)
            .username(username)
            .action("EXPORT")
            .entityType(entityType)
            .description("Exported " + recordCount + " records from " + entityType)
            .success(true)
            .build();
        log(entry);
    }

    /**
     * 查询用户操作日志
     */
    public List<AuditLog> getUserLogs(String userId) {
        return auditLogRepository.findByUserIdOrderByTimestampDesc(userId);
    }

    /**
     * 查询实体操作日志
     */
    public List<AuditLog> getEntityLogs(String entityType, String entityId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(entityType, entityId);
    }

    /**
     * 清理旧日志（保留指定天数）
     */
    public void cleanupOldLogs(int daysToKeep) {
        LocalDateTime beforeDate = LocalDateTime.now().minusDays(daysToKeep);
        long deletedCount = auditLogRepository.countByTimestampBefore(beforeDate);
        auditLogRepository.deleteOldLogs(beforeDate);
        log.info("Deleted {} old audit logs (older than {} days)", deletedCount, daysToKeep);
    }

    /**
     * 构建审计日志实体
     */
    private AuditLog buildAuditLog(AuditLogEntry entry) {
        ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        String ipAddress = null;
        String userAgent = null;

        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            ipAddress = getClientIp(request);
            userAgent = request.getHeader("User-Agent");
        }

        return AuditLog.builder()
            .userId(truncate(entry.getUserId(), 255))
            .username(truncate(entry.getUsername(), 100))
            .action(truncate(entry.getAction(), 50))
            .entityType(truncate(entry.getEntityType(), 100))
            .entityId(truncate(entry.getEntityId(), 100))
            .description(truncate(entry.getDescription(), 500))
            .oldValue(entry.getOldValue())
            .newValue(entry.getNewValue())
            .ipAddress(truncate(ipAddress, 50))
            .userAgent(truncate(userAgent, 500))
            .success(entry.getSuccess() != null ? entry.getSuccess() : true)
            .errorMessage(entry.getErrorMessage())
            .timestamp(LocalDateTime.now())
            .build();
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }

        // 处理多个IP的情况（X-Forwarded-For可能包含多个IP）
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    /**
     * 将对象转换为JSON字符串
     */
    private String toJsonString(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof String) {
            return (String) obj;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize object to JSON", e);
            return obj.toString();
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    /**
     * 审计日志条目构建器
     */
    @lombok.Builder
    @lombok.Data
    public static class AuditLogEntry {
        private String userId;
        private String username;
        private String action;
        private String entityType;
        private String entityId;
        private String description;
        private String oldValue;
        private String newValue;
        private Boolean success;
        private String errorMessage;
    }
}
