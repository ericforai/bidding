package com.xiyu.bid.fees.repository;

import com.xiyu.bid.fees.entity.Fee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * 费用数据访问接口
 */
@Repository
public interface FeeRepository extends JpaRepository<Fee, Long> {

    /**
     * 根据项目ID查询费用列表
     */
    List<Fee> findByProjectId(Long projectId);

    /**
     * 根据状态查询费用列表
     */
    List<Fee> findByStatus(Fee.Status status);

    /**
     * 根据项目ID和状态查询费用列表
     */
    List<Fee> findByProjectIdAndStatus(Long projectId, Fee.Status status);

    /**
     * 根据费用类型查询费用列表
     */
    List<Fee> findByFeeType(Fee.FeeType feeType);

    /**
     * 计算指定项目和状态的费用总额
     */
    @Query("SELECT COALESCE(SUM(f.amount), 0) FROM Fee f WHERE f.projectId = :projectId AND f.status = :status")
    BigDecimal sumAmountByProjectIdAndStatus(@Param("projectId") Long projectId, @Param("status") Fee.Status status);

    /**
     * 计算指定项目的所有费用总额
     */
    @Query("SELECT COALESCE(SUM(f.amount), 0) FROM Fee f WHERE f.projectId = :projectId")
    BigDecimal sumAmountByProjectId(@Param("projectId") Long projectId);

    /**
     * 分页查询所有费用
     */
    Page<Fee> findAll(Pageable pageable);

    /**
     * 根据项目ID分页查询费用
     */
    Page<Fee> findByProjectId(Long projectId, Pageable pageable);
}
