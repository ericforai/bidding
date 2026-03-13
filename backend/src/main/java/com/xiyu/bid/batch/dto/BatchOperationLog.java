package com.xiyu.bid.batch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 批量操作日志DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchOperationLog {

    /**
     * 日志ID
     */
    private Long id;

    /**
     * 操作类型 (CLAIM, ASSIGN, DELETE, etc.)
     */
    private String operationType;

    /**
     * 目标项目类型 (TENDER, TASK, PROJECT, etc.)
     */
    private String itemType;

    /**
     * 操作的项目ID列表 (JSON格式)
     */
    private String itemIds;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人用户名
     */
    private String operatorName;

    /**
     * 成功数量
     */
    private Integer successCount;

    /**
     * 失败数量
     */
    private Integer failureCount;

    /**
     * 操作结果状态 (SUCCESS, PARTIAL_SUCCESS, FAILED)
     */
    private String status;

    /**
     * 错误详情 (JSON格式)
     */
    private String errorDetails;

    /**
     * 操作时间
     */
    private LocalDateTime operationTime;

    /**
     * 客户端IP地址
     */
    private String clientIp;

    /**
     * 附加信息 (JSON格式)
     */
    private String metadata;
}
