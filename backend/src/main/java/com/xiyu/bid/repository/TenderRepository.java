package com.xiyu.bid.repository;

import com.xiyu.bid.entity.Tender;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 标讯数据访问接口
 */
@Repository
public interface TenderRepository extends JpaRepository<Tender, Long>, JpaSpecificationExecutor<Tender> {

    /**
     * 根据外部 ID 查询标讯
     */
    java.util.Optional<Tender> findByExternalId(String externalId);

    /**
     * 根据状态查询标讯
     */
    List<Tender> findByStatus(Tender.Status status);

    /**
     * 根据来源查询标讯
     */
    List<Tender> findBySource(String source);

    /**
     * 统计指定状态的标讯数量
     */
    Long countByStatus(Tender.Status status);

    /**
     * 根据AI评分范围查询标讯
     */
    List<Tender> findByAiScoreBetween(Integer minScore, Integer maxScore);

    /**
     * 根据风险等级查询标讯
     */
    List<Tender> findByRiskLevel(Tender.RiskLevel riskLevel);
}
