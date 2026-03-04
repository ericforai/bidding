package com.xiyu.bid.dto;

import com.xiyu.bid.entity.Case;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 案例数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseDTO {

    private Long id;
    private String title;
    private Case.Industry industry;
    private Case.Outcome outcome;
    private BigDecimal amount;
    private LocalDate projectDate;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
