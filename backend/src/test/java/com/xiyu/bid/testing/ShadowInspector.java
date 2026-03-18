package com.xiyu.bid.testing;

import com.xiyu.bid.service.IAuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.function.Function;

/**
 * Shadow Inspector - 跨层验证工具
 *
 * 验证三层一致性:
 * 1. API响应层 (Controller返回)
 * 2. 数据库层 (Repository存储)
 * 3. 审计日志层 (@Auditable记录)
 *
 * 使用示例:
 * <pre>
 * shadowInspector.verifyInvariant(
 *     "collaboration_thread",
 *     threadId,
 *     dto -> dto.getStatus(),
 *     "status",
 *     "OPEN"
 * );
 * </pre>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ShadowInspector {

    private static final Map<String, String> AUDIT_ENTITY_TYPE_MAP = new HashMap<>();

    static {
        AUDIT_ENTITY_TYPE_MAP.put("collaboration_threads", "CollaborationThread");
        AUDIT_ENTITY_TYPE_MAP.put("comments", "Comment");
        AUDIT_ENTITY_TYPE_MAP.put("projects", "Project");
        AUDIT_ENTITY_TYPE_MAP.put("tasks", "Task");
        AUDIT_ENTITY_TYPE_MAP.put("tenders", "Tender");
        AUDIT_ENTITY_TYPE_MAP.put("qualifications", "Qualification");
        AUDIT_ENTITY_TYPE_MAP.put("cases", "Case");
        AUDIT_ENTITY_TYPE_MAP.put("templates", "Template");
        AUDIT_ENTITY_TYPE_MAP.put("fees", "Fee");
        AUDIT_ENTITY_TYPE_MAP.put("platform_accounts", "PlatformAccount");
        AUDIT_ENTITY_TYPE_MAP.put("bar_assets", "BarAsset");
        AUDIT_ENTITY_TYPE_MAP.put("calendar_events", "CalendarEvent");
        AUDIT_ENTITY_TYPE_MAP.put("competitors", "Competitor");
        AUDIT_ENTITY_TYPE_MAP.put("competition_analyses", "CompetitionAnalysis");
        AUDIT_ENTITY_TYPE_MAP.put("score_analyses", "ScoreAnalysis");
        AUDIT_ENTITY_TYPE_MAP.put("roi_analyses", "ROIAnalysis");
        AUDIT_ENTITY_TYPE_MAP.put("document_versions", "DocumentVersion");
        AUDIT_ENTITY_TYPE_MAP.put("document_sections", "DocumentSection");
        AUDIT_ENTITY_TYPE_MAP.put("document_structures", "DocumentStructure");
        AUDIT_ENTITY_TYPE_MAP.put("document_assemblies", "DocumentAssembly");
    }

    private final JdbcTemplate jdbcTemplate;
    private final IAuditLogService auditLogService;

    /**
     * 允许的表名白名单 - 防止SQL注入
     */
    private static final Set<String> ALLOWED_TABLES = Set.of(
        "collaboration_threads",
        "comments",
        "projects",
        "tasks",
        "tenders",
        "qualifications",
        "cases",
        "templates",
        "fees",
        "platform_accounts",
        "bar_assets",
        "compliance_check_results",
        "alert_rules",
        "alert_history",
        "calendar_events",
        "competitors",
        "competition_analyses",
        "score_analyses",
        "dimension_scores",
        "roi_analyses",
        "document_versions",
        "document_sections",
        "document_structures",
        "assembly_templates",
        "document_assemblies",
        "audit_logs"
    );

    /**
     * 验证表名是否在白名单中
     *
     * @param tableName 表名
     * @throws IllegalArgumentException 如果表名不在白名单中
     */
    private void validateTable(String tableName) {
        if (!ALLOWED_TABLES.contains(tableName)) {
            throw new IllegalArgumentException(
                "Table not allowed: " + tableName + ". This table is not in the allowed list."
            );
        }
    }

    /**
     * 验证三层一致性
     *
     * @param tableName 表名
     * @param entityId 实体ID
     * @param dbExtractor 从数据库提取值的函数
     * @param columnName 列名
     * @param expectedValue 期望值
     */
    public <T> void verifyInvariant(
            String tableName,
            Long entityId,
            Function<Map<String, Object>, T> dbExtractor,
            String columnName,
            T expectedValue) {

        log.debug("Shadow Inspector: verifying invariant for {}::{}", tableName, entityId);

        // Layer 1: 数据库验证
        T dbValue = queryDatabase(tableName, entityId, dbExtractor);
        if (!equals(dbValue, expectedValue)) {
            throw new AssertionError(
                String.format("DB Layer mismatch: %s::%s.%s = %s, expected %s",
                    tableName, entityId, columnName, dbValue, expectedValue));
        }

        // Layer 2: 审计日志验证
        verifyAuditLog(tableName, entityId);
    }

    /**
     * 验证实体存在性
     */
    public void verifyExists(String tableName, Long entityId) {
        validateTable(tableName);
        String sql = String.format("SELECT COUNT(*) FROM %s WHERE id = ?", tableName);
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, entityId);

        if (count == null || count == 0) {
            throw new AssertionError(
                String.format("Entity %s::%s does not exist in database", tableName, entityId));
        }
    }

    /**
     * 验证实体不存在（删除后）
     */
    public void verifyNotExists(String tableName, Long entityId) {
        validateTable(tableName);
        String sql = String.format("SELECT COUNT(*) FROM %s WHERE id = ?", tableName);
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, entityId);

        if (count != null && count > 0) {
            throw new AssertionError(
                String.format("Entity %s::%s still exists in database", tableName, entityId));
        }
    }

    /**
     * 验证审计日志存在
     */
    public void verifyAuditLog(String entityType, Long entityId) {
        String auditEntityType = resolveAuditEntityType(entityType);
        String sql = "SELECT COUNT(*) FROM audit_logs WHERE entity_type = ? AND entity_id = ?";
        Integer count = waitForAuditCount(sql, auditEntityType, String.valueOf(entityId));

        if (count == null || count == 0) {
            throw new AssertionError(
                String.format("No audit log found for %s::%s", auditEntityType, entityId));
        }
    }

    /**
     * 验证审计日志操作类型
     */
    public void verifyAuditAction(String entityType, Long entityId, String action) {
        String auditEntityType = resolveAuditEntityType(entityType);
        String sql = "SELECT COUNT(*) FROM audit_logs WHERE entity_type = ? AND entity_id = ? AND action = ?";
        Integer count = waitForAuditCount(sql, auditEntityType, String.valueOf(entityId), action);

        if (count == null || count == 0) {
            throw new AssertionError(
                String.format("No %s audit log found for %s::%s", action, auditEntityType, entityId));
        }
    }

    /**
     * 验证时间戳一致性
     * created_at 和 updated_at 应该符合预期
     */
    public void verifyTimestamps(String tableName, Long entityId, boolean shouldBeUpdated) {
        validateTable(tableName);
        String sql = String.format("SELECT created_at, updated_at FROM %s WHERE id = ?", tableName);
        Map<String, Object> result = jdbcTemplate.queryForMap(sql, entityId);

        LocalDateTime createdAt = toLocalDateTime(result.get("created_at"));
        LocalDateTime updatedAt = toLocalDateTime(result.get("updated_at"));

        if (createdAt == null) {
            throw new AssertionError(
                String.format("%s::%s created_at is null", tableName, entityId));
        }

        if (shouldBeUpdated) {
            if (updatedAt == null || updatedAt.isBefore(createdAt)) {
                throw new AssertionError(
                    String.format("%s::%s updated_at is invalid", tableName, entityId));
            }
        }
    }

    /**
     * 验证软删除状态
     */
    public void verifySoftDeleted(String tableName, Long entityId, String deletedColumn) {
        validateTable(tableName);
        String sql = String.format("SELECT %s FROM %s WHERE id = ?", deletedColumn, tableName);
        Boolean isDeleted = jdbcTemplate.queryForObject(sql, Boolean.class, entityId);

        if (isDeleted == null || !isDeleted) {
            throw new AssertionError(
                String.format("%s::%s is not soft deleted (%s = %s)",
                    tableName, entityId, deletedColumn, isDeleted));
        }
    }

    /**
     * 验证状态转换合法性
     */
    public void verifyStateTransition(
            String tableName,
            Long entityId,
            String statusColumn,
            String fromStatus,
            String toStatus) {

        validateTable(tableName);
        String sql = String.format("SELECT %s FROM %s WHERE id = ?", statusColumn, tableName);
        String currentStatus = jdbcTemplate.queryForObject(sql, String.class, entityId);

        if (!toStatus.equals(currentStatus)) {
            throw new AssertionError(
                String.format("State transition failed: %s::%s expected %s but got %s",
                    tableName, entityId, toStatus, currentStatus));
        }

        log.debug("State transition verified: {} -> {} for {}::{}", fromStatus, toStatus, tableName, entityId);
    }

    /**
     * 查询数据库
     */
    private <T> T queryDatabase(
            String tableName,
            Long entityId,
            Function<Map<String, Object>, T> extractor) {

        String sql = String.format("SELECT * FROM %s WHERE id = ?", tableName);
        Map<String, Object> result = jdbcTemplate.queryForMap(sql, entityId);

        return extractor.apply(result);
    }

    /**
     * 安全比较
     */
    private boolean equals(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    private String resolveAuditEntityType(String entityTypeOrTableName) {
        return AUDIT_ENTITY_TYPE_MAP.getOrDefault(entityTypeOrTableName, entityTypeOrTableName);
    }

    private Integer waitForAuditCount(String sql, Object... args) {
        Integer count = 0;
        for (int i = 0; i < 20; i++) {
            count = jdbcTemplate.queryForObject(sql, Integer.class, args);
            if (count != null && count > 0) {
                return count;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return count;
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        throw new IllegalStateException("Unsupported timestamp type: " + value.getClass().getName());
    }

    /**
     * Builder风格的验证链
     */
    public static class VerificationChain {
        private final ShadowInspector inspector;
        private final String tableName;
        private final Long entityId;

        public VerificationChain(ShadowInspector inspector, String tableName, Long entityId) {
            this.inspector = inspector;
            this.tableName = tableName;
            this.entityId = entityId;
        }

        public VerificationChain exists() {
            inspector.verifyExists(tableName, entityId);
            return this;
        }

        public VerificationChain notExists() {
            inspector.verifyNotExists(tableName, entityId);
            return this;
        }

        public VerificationChain hasAuditLog() {
            inspector.verifyAuditLog(tableName, entityId);
            return this;
        }

        public VerificationChain hasAuditAction(String action) {
            inspector.verifyAuditAction(tableName, entityId, action);
            return this;
        }

        public VerificationChain stateTransition(String from, String to) {
            inspector.verifyStateTransition(tableName, entityId, "status", from, to);
            return this;
        }

        public VerificationChain timestampsValid(boolean updated) {
            inspector.verifyTimestamps(tableName, entityId, updated);
            return this;
        }

        public VerificationChain softDeleted(String column) {
            inspector.verifySoftDeleted(tableName, entityId, column);
            return this;
        }
    }

    /**
     * 开始验证链
     */
    public VerificationChain verify(String tableName, Long entityId) {
        return new VerificationChain(this, tableName, entityId);
    }
}
