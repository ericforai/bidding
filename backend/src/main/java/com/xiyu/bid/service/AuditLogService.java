// Input: Repository, 相关依赖
// Output: 业务服务、数据操作
// Pos: Service/业务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.xiyu.bid.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiyu.bid.audit.dto.AuditLogItemDTO;
import com.xiyu.bid.audit.dto.AuditLogQueryResponse;
import com.xiyu.bid.audit.dto.AuditLogSummaryDTO;
import com.xiyu.bid.entity.AuditLog;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.repository.AuditLogRepository;
import com.xiyu.bid.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 审计日志服务
 * 异步记录系统中的关键操作
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService implements IAuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private static final DateTimeFormatter AUDIT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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

    public AuditLogQueryResponse queryLogs(String keyword,
                                           String action,
                                           String module,
                                           String operator,
                                           LocalDateTime start,
                                           LocalDateTime end,
                                           Boolean success) {
        List<AuditLogItemDTO> items = auditLogRepository.findAll(Sort.by(Sort.Direction.DESC, "timestamp")).stream()
                .filter(auditLog -> matchesKeyword(auditLog, keyword))
                .filter(auditLog -> matchesAction(auditLog, action))
                .filter(auditLog -> matchesOperator(auditLog, operator))
                .filter(auditLog -> matchesStart(auditLog, start))
                .filter(auditLog -> matchesEnd(auditLog, end))
                .filter(auditLog -> matchesSuccess(auditLog, success))
                .map(this::toItemDto)
                .filter(item -> module == null || module.isBlank() || module.equalsIgnoreCase(item.getModule()))
                .collect(Collectors.toList());

        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime weekStart = LocalDateTime.now().minusDays(6).toLocalDate().atStartOfDay();

        AuditLogSummaryDTO summary = AuditLogSummaryDTO.builder()
                .todayCount(items.stream().filter(item -> parseAuditTime(item.getTime()).isAfter(todayStart.minusSeconds(1))).count())
                .weekCount(items.stream().filter(item -> parseAuditTime(item.getTime()).isAfter(weekStart.minusSeconds(1))).count())
                .failedCount(items.stream().filter(item -> "failed".equals(item.getStatus())).count())
                .activeUserCount(items.stream().map(AuditLogItemDTO::getOperator).filter(Objects::nonNull).distinct().count())
                .totalCount(items.size())
                .build();

        return AuditLogQueryResponse.builder()
                .items(items)
                .summary(summary)
                .build();
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

    private AuditLogItemDTO toItemDto(AuditLog auditLog) {
        User user = resolveUser(auditLog);
        return AuditLogItemDTO.builder()
                .id(auditLog.getId())
                .time(auditLog.getTimestamp() == null ? null : auditLog.getTimestamp().format(AUDIT_TIME_FORMATTER))
                .operator(resolveOperator(auditLog, user))
                .department("-")
                .role(resolveRole(user))
                .actionType(normalizeAction(auditLog.getAction()))
                .module(normalizeModule(auditLog.getEntityType()))
                .target(resolveTarget(auditLog))
                .detail(auditLog.getDescription())
                .ip(auditLog.getIpAddress())
                .status(auditLog.getSuccess() ? "success" : "failed")
                .build();
    }

    private User resolveUser(AuditLog auditLog) {
        if (auditLog.getUserId() != null && auditLog.getUserId().chars().allMatch(Character::isDigit)) {
            return userRepository.findById(Long.parseLong(auditLog.getUserId())).orElse(null);
        }
        if (auditLog.getUsername() != null && !auditLog.getUsername().isBlank()) {
            return userRepository.findByUsername(auditLog.getUsername()).orElse(null);
        }
        return null;
    }

    private String resolveOperator(AuditLog auditLog, User user) {
        if (user != null && user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName();
        }
        if (auditLog.getUsername() != null && !auditLog.getUsername().isBlank()) {
            return auditLog.getUsername();
        }
        return "未知用户";
    }

    private String resolveRole(User user) {
        if (user == null || user.getRole() == null) {
            return "unknown";
        }
        return user.getRole().name().toLowerCase(Locale.ROOT);
    }

    private String normalizeAction(String action) {
        return action == null ? "unknown" : action.toLowerCase(Locale.ROOT);
    }

    private String normalizeModule(String entityType) {
        if (entityType == null || entityType.isBlank()) {
            return "system";
        }
        String normalized = entityType.toLowerCase(Locale.ROOT);
        if (normalized.contains("project")) {
            return "project";
        }
        if (normalized.contains("tender") || normalized.contains("bidding")) {
            return "bidding";
        }
        if (normalized.contains("qualification")) {
            return "qualification";
        }
        if (normalized.contains("expense")) {
            return "expense";
        }
        if (normalized.contains("account") || normalized.contains("bar")) {
            return "account";
        }
        if (normalized.contains("template") || normalized.contains("case")) {
            return "knowledge";
        }
        if (normalized.contains("analytics") || normalized.contains("ai")) {
            return "analytics";
        }
        if (normalized.contains("document") || normalized.contains("archive") || normalized.contains("export")) {
            return "document";
        }
        return "system";
    }

    private String resolveTarget(AuditLog auditLog) {
        if (auditLog.getEntityId() != null && !auditLog.getEntityId().isBlank()) {
            return auditLog.getEntityId();
        }
        if (auditLog.getEntityType() != null && !auditLog.getEntityType().isBlank()) {
            return auditLog.getEntityType();
        }
        return "-";
    }

    private LocalDateTime parseAuditTime(String time) {
        if (time == null || time.isBlank()) {
            return LocalDateTime.MIN;
        }
        return LocalDateTime.parse(time, AUDIT_TIME_FORMATTER);
    }

    private boolean matchesKeyword(AuditLog auditLog, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String normalizedKeyword = keyword.toLowerCase(Locale.ROOT);
        return containsIgnoreCase(auditLog.getDescription(), normalizedKeyword)
                || containsIgnoreCase(auditLog.getEntityId(), normalizedKeyword)
                || containsIgnoreCase(auditLog.getEntityType(), normalizedKeyword)
                || containsIgnoreCase(auditLog.getUsername(), normalizedKeyword);
    }

    private boolean matchesAction(AuditLog auditLog, String action) {
        if (action == null || action.isBlank()) {
            return true;
        }
        return action.equalsIgnoreCase(auditLog.getAction());
    }

    private boolean matchesOperator(AuditLog auditLog, String operator) {
        if (operator == null || operator.isBlank()) {
            return true;
        }
        return operator.equalsIgnoreCase(auditLog.getUsername());
    }

    private boolean matchesStart(AuditLog auditLog, LocalDateTime start) {
        return start == null || (auditLog.getTimestamp() != null && !auditLog.getTimestamp().isBefore(start));
    }

    private boolean matchesEnd(AuditLog auditLog, LocalDateTime end) {
        return end == null || (auditLog.getTimestamp() != null && !auditLog.getTimestamp().isAfter(end));
    }

    private boolean matchesSuccess(AuditLog auditLog, Boolean success) {
        return success == null || Objects.equals(auditLog.getSuccess(), success);
    }

    private boolean containsIgnoreCase(String source, String normalizedKeyword) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(normalizedKeyword);
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
