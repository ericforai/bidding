package com.xiyu.bid.fees.dto;

import com.xiyu.bid.fees.entity.Fee;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 费用数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeDTO {

    private Long id;
    private Long projectId;
    private Fee.FeeType feeType;
    private BigDecimal amount;
    private LocalDateTime feeDate;
    private Fee.Status status;
    private LocalDateTime paymentDate;
    private LocalDateTime returnDate;
    private String paidBy;
    private String returnTo;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
