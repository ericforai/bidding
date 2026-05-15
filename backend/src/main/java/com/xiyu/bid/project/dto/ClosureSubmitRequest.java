// Input: 结项提交 HTTP 请求体
// Output: 结项服务所需入参（保证金退回登记 + 归档信息）
// Pos: project/dto/
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClosureSubmitRequest {
    /** 是否已登记保证金退回（PRD §3.6 核心门禁所需）。 */
    private Boolean depositReturned;
    /** 保证金退回日期。 */
    private LocalDateTime depositReturnDate;
    /** 保证金退回凭证文档 ID。 */
    private Long depositReturnEvidenceId;
    /** 归档位置（可选）。 */
    private String archiveLocation;
    /** 结项备注（可选）。 */
    private String notes;
}
