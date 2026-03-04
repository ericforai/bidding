package com.xiyu.bid.repository;

import com.xiyu.bid.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审计日志数据访问层
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * 查询用户的所有操作日志
     */
    List<AuditLog> findByUserIdOrderByTimestampDesc(String userId);

    /**
     * 查询指定实体的操作日志
     */
    List<AuditLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(
        String entityType, String entityId
    );

    /**
     * 查询指定类型的操作日志
     */
    List<AuditLog> findByActionOrderByTimestampDesc(String action);

    /**
     * 查询时间范围内的日志
     */
    List<AuditLog> findByTimestampBetweenOrderByTimestampDesc(
        LocalDateTime start, LocalDateTime end
    );

    /**
     * 查询失败的操作日志
     */
    List<AuditLog> findBySuccessFalseOrderByTimestampDesc();

    /**
     * 统计用户在指定时间内的操作次数
     */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.userId = :userId " +
           "AND a.timestamp BETWEEN :start AND :end")
    long countByUserIdAndTimestampBetween(
        @Param("userId") String userId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    /**
     * 统计指定时间之前的日志数量
     */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.timestamp < :beforeDate")
    long countByTimestampBefore(@Param("beforeDate") LocalDateTime beforeDate);

    /**
     * 删除旧的日志（用于定期清理）
     */
    @Query("DELETE FROM AuditLog a WHERE a.timestamp < :beforeDate")
    void deleteOldLogs(@Param("beforeDate") LocalDateTime beforeDate);
}
