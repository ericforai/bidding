package com.xiyu.bid.repository;

import com.xiyu.bid.entity.Case;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 案例Repository接口
 */
@Repository
public interface CaseRepository extends JpaRepository<Case, Long> {

    /**
     * 根据行业查找案例（分页）
     */
    Page<Case> findByIndustry(Case.Industry industry, Pageable pageable);

    /**
     * 根据结果查找案例（分页）
     */
    Page<Case> findByOutcome(Case.Outcome outcome, Pageable pageable);

    /**
     * 根据标题查找案例（模糊查询，分页）
     */
    Page<Case> findByTitleContaining(String title, Pageable pageable);

    /**
     * 根据金额范围查找案例（分页）
     */
    Page<Case> findByAmountBetween(BigDecimal minAmount, BigDecimal maxAmount, Pageable pageable);

    /**
     * 根据项目日期范围查找案例（分页）
     */
    Page<Case> findByProjectDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);

    /**
     * 根据行业和结果查找案例（分页）
     */
    Page<Case> findByIndustryAndOutcome(Case.Industry industry, Case.Outcome outcome, Pageable pageable);

    /**
     * 统计行业的案例数量
     */
    Long countByIndustry(Case.Industry industry);

    /**
     * 统计结果的案例数量
     */
    Long countByOutcome(Case.Outcome outcome);

    /**
     * 查找金额大于指定值的案例，按金额降序排序（分页）
     */
    Page<Case> findByAmountGreaterThanOrderByAmountDesc(BigDecimal amount, Pageable pageable);

    /**
     * 查找指定日期之后的案例（分页）
     */
    Page<Case> findByProjectDateAfter(LocalDate date, Pageable pageable);

    /**
     * 根据行业查找案例（限制返回数量）
     */
    List<Case> findByIndustry(Case.Industry industry);

    /**
     * 根据结果查找案例（限制返回数量）
     */
    List<Case> findByOutcome(Case.Outcome outcome);
}
