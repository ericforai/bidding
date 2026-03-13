package com.xiyu.bid.batch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 批量操作响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchOperationResponse {

    /**
     * 操作是否成功
     */
    private Boolean success;

    /**
     * 成功处理的项目数量
     */
    @Builder.Default
    private Integer successCount = 0;

    /**
     * 失败的项目数量
     */
    @Builder.Default
    private Integer failureCount = 0;

    /**
     * 总项目数量
     */
    @Builder.Default
    private Integer totalCount = 0;

    /**
     * 成功的项目ID列表
     */
    @Builder.Default
    private List<Long> successIds = new ArrayList<>();

    /**
     * 失败的项目ID及错误信息
     */
    @Builder.Default
    private List<BatchOperationError> errors = new ArrayList<>();

    /**
     * 操作时间
     */
    private LocalDateTime operationTime;

    /**
     * 操作类型
     */
    private String operationType;

    /**
     * 错误详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchOperationError {
        /**
         * 失败的项目ID
         */
        private Long itemId;

        /**
         * 错误信息
         */
        private String errorMessage;

        /**
         * 错误代码
         */
        private String errorCode;
    }

    /**
     * 添加成功记录
     */
    public void addSuccess(Long id) {
        this.successIds.add(id);
        this.successCount++;
    }

    /**
     * 添加失败记录
     */
    public void addError(Long id, String errorMessage, String errorCode) {
        this.errors.add(BatchOperationError.builder()
                .itemId(id)
                .errorMessage(errorMessage)
                .errorCode(errorCode)
                .build());
        this.failureCount++;
    }

    /**
     * 设置总数
     */
    public void setTotalCount(int count) {
        this.totalCount = count;
    }

    /**
     * 判断是否全部成功
     */
    public boolean isAllSuccess() {
        return this.successCount.equals(this.totalCount) && this.totalCount > 0;
    }

    /**
     * 获取失败率百分比
     */
    public double getFailureRate() {
        if (totalCount == 0) {
            return 0.0;
        }
        return (double) failureCount / totalCount * 100;
    }
}
