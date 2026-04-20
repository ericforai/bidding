// Input: TenderRepository, CustomerPredictionRepository, core policies
// Output: Customer insights, purchases, predictions, and refresh operations
// Pos: Service/编排层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.marketinsight.service;

import com.xiyu.bid.entity.Tender;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.marketinsight.core.IndustryClassificationPolicy;
import com.xiyu.bid.marketinsight.core.OpportunityScoringPolicy;
import com.xiyu.bid.marketinsight.core.PredictionTransitionPolicy;
import com.xiyu.bid.marketinsight.core.PurchaserExtractionPolicy;
import com.xiyu.bid.marketinsight.dto.CustomerInsightDTO;
import com.xiyu.bid.marketinsight.dto.CustomerOpportunityAssembler;
import com.xiyu.bid.marketinsight.dto.CustomerPredictionDTO;
import com.xiyu.bid.marketinsight.dto.CustomerPurchaseDTO;
import com.xiyu.bid.marketinsight.entity.CustomerPrediction;
import com.xiyu.bid.marketinsight.repository.CustomerPredictionRepository;
import com.xiyu.bid.repository.TenderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 客户商机服务 — 纯编排层
 * 加载 → 转换为核心类型 → 调用核心策略 → 映射为 DTO → 返回
 */
@Service
@RequiredArgsConstructor
public class CustomerOpportunityService {

    private final TenderRepository tenderRepository;
    private final CustomerPredictionRepository customerPredictionRepository;

    /** Intermediate record carrying purchaser and industry metadata. */
    record EnrichedTender(Tender tender, String purchaserName,
                          String purchaserHash, String industry) {
    }

    /**
     * 获取所有客户洞察列表。
     *
     * @return 客户洞察 DTO 列表
     */
    @Transactional(readOnly = true)
    public List<CustomerInsightDTO> getCustomerInsights() {
        return customerPredictionRepository.findAllByOrderByOpportunityScoreDesc()
                .stream()
                .map(CustomerOpportunityAssembler::toInsightDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取指定采购人的采购记录。
     *
     * @param purchaserHash 采购人哈希值
     * @return 客户采购记录 DTO 列表
     */
    @Transactional(readOnly = true)
    public List<CustomerPurchaseDTO> getCustomerPurchases(String purchaserHash) {
        List<Tender> tenders = tenderRepository.findAll();

        return tenders.stream()
                .filter(t -> {
                    var extraction = PurchaserExtractionPolicy.extractPurchaser(t.getTitle());
                    return extraction.found() && extraction.purchaserHash().equals(purchaserHash);
                })
                .map(t -> {
                    String industry = IndustryClassificationPolicy.classifyIndustry(t.getTitle());
                    return CustomerOpportunityAssembler.toPurchaseDTO(t, purchaserHash, industry);
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取指定采购人的商机预测。
     *
     * @param purchaserHash 采购人哈希值
     * @return 客户商机预测 DTO 列表
     */
    @Transactional(readOnly = true)
    public List<CustomerPredictionDTO> getCustomerPredictions(String purchaserHash) {
        return customerPredictionRepository.findByPurchaserHash(purchaserHash)
                .stream()
                .map(CustomerOpportunityAssembler::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 重新计算所有客户商机预测。
     * 加载全部标讯 → 提取采购人/行业 → 按采购人分组 → 计算评分/窗口/周期 → 更新或新建预测记录
     */
    @Transactional
    public void refreshInsights() {
        List<Tender> tenders = tenderRepository.findAll();
        List<EnrichedTender> enriched = enrichTenders(tenders);

        Map<String, List<EnrichedTender>> byPurchaser = new LinkedHashMap<>();
        for (EnrichedTender et : enriched) {
            if (et.purchaserHash() != null && !et.purchaserHash().isBlank()) {
                byPurchaser.computeIfAbsent(et.purchaserHash(), k -> new ArrayList<>()).add(et);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        List<CustomerPrediction> predictions = new ArrayList<>(byPurchaser.size());
        for (var entry : byPurchaser.entrySet()) {
            predictions.add(buildPrediction(entry.getKey(), entry.getValue(), now));
        }
        customerPredictionRepository.saveAll(predictions);

        customerPredictionRepository.findAll().stream()
                .filter(p -> p.getStatus() != CustomerPrediction.Status.CONVERTED)
                .filter(p -> !byPurchaser.containsKey(p.getPurchaserHash()))
                .forEach(customerPredictionRepository::delete);
    }

    private CustomerPrediction buildPrediction(final String hash,
                                               final List<EnrichedTender> group,
                                               final LocalDateTime now) {
        String name = group.get(0).purchaserName();
        String industry = group.get(0).industry();
        int frequency = group.size();
        BigDecimal avgBudgetYuan = computeAvgBudgetYuan(group);
        long avgBudgetWan = avgBudgetYuan.divide(BigDecimal.valueOf(10_000L),
                0, java.math.RoundingMode.HALF_UP).longValue();
        int monthsSinceLast = computeMonthsSinceLast(group, now);

        Map<Integer, Integer> monthCounts = new LinkedHashMap<>();
        for (EnrichedTender et : group) {
            LocalDateTime created = et.tender().getCreatedAt();
            if (created != null) {
                monthCounts.merge(created.getMonthValue(), 1, Integer::sum);
            }
        }

        String cycleType = OpportunityScoringPolicy.classifyCycleType(monthCounts);
        boolean hasCycle = "年度集中采购".equals(cycleType) || "季度规律采购".equals(cycleType);
        var score = OpportunityScoringPolicy.computeScore(frequency, monthsSinceLast, avgBudgetWan, hasCycle);
        var window = OpportunityScoringPolicy.predictNextWindow(monthCounts, now.getMonthValue());
        String evidenceIds = group.stream()
                .map(et -> String.valueOf(et.tender().getId()))
                .collect(Collectors.joining(","));
        String mainCategories = group.stream()
                .map(EnrichedTender::industry)
                .distinct()
                .collect(Collectors.joining(","));
        String periodMonths = monthCounts.keySet().stream()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        BigDecimal predictedMin = avgBudgetYuan.multiply(BigDecimal.valueOf(0.8));
        BigDecimal predictedMax = avgBudgetYuan.multiply(BigDecimal.valueOf(1.2));
        String reasoning = OpportunityScoringPolicy.generateReasoningSummary(
                name, industry, frequency, industry, window.confidence());

        CustomerPrediction prediction = customerPredictionRepository.findByPurchaserHash(hash)
                .stream().findFirst()
                .orElseGet(() -> CustomerPrediction.builder()
                        .purchaserHash(hash).purchaserName(name).build());

        prediction.setPurchaserName(name);
        prediction.setIndustry(industry);
        prediction.setOpportunityScore(score.total());
        prediction.setPredictedCategory(industry);
        prediction.setPredictedBudgetMin(predictedMin);
        prediction.setPredictedBudgetMax(predictedMax);
        prediction.setPredictedWindow(window.windowLabel());
        prediction.setConfidence(BigDecimal.valueOf(window.confidence()));
        prediction.setReasoningSummary(reasoning);
        prediction.setEvidenceRecordIds(evidenceIds);
        prediction.setMainCategories(mainCategories);
        prediction.setAvgBudget(avgBudgetYuan);
        prediction.setCycleType(cycleType);
        prediction.setFrequency(frequency);
        prediction.setPeriodMonths(periodMonths);
        prediction.setLastComputedAt(now);
        return prediction;
    }

    /**
     * 转换预测状态。
     * 委托 PredictionTransitionPolicy 校验合法性后更新状态。
     *
     * @param id           预测记录 ID
     * @param targetStatus 目标状态名称
     * @return 更新后的预测 DTO
     */
    @Transactional
    public CustomerPredictionDTO transitionPrediction(Long id, String targetStatus) {
        CustomerPrediction prediction = customerPredictionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CustomerPrediction", String.valueOf(id)));

        PredictionTransitionPolicy.PredictionStatus coreCurrent =
                mapToCoreStatus(prediction.getStatus());
        PredictionTransitionPolicy.PredictionStatus coreTarget =
                PredictionTransitionPolicy.PredictionStatus.valueOf(targetStatus);

        var result = PredictionTransitionPolicy.validateTransition(coreCurrent, coreTarget);
        if (!result.allowed()) {
            throw new IllegalStateException(result.reason());
        }

        prediction.setStatus(CustomerPrediction.Status.valueOf(targetStatus));
        CustomerPrediction saved = customerPredictionRepository.save(prediction);
        return CustomerOpportunityAssembler.toDTO(saved);
    }

    /**
     * 将预测转化为项目。
     *
     * @param id        预测记录 ID
     * @param projectId 关联项目 ID
     * @return 更新后的预测 DTO
     */
    @Transactional
    public CustomerPredictionDTO convertPrediction(Long id, Long projectId) {
        CustomerPrediction prediction = customerPredictionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CustomerPrediction", String.valueOf(id)));

        PredictionTransitionPolicy.PredictionStatus coreCurrent =
                mapToCoreStatus(prediction.getStatus());
        var result = PredictionTransitionPolicy.validateTransition(
                coreCurrent, PredictionTransitionPolicy.PredictionStatus.CONVERTED);
        if (!result.allowed()) {
            throw new IllegalStateException(result.reason());
        }

        prediction.setStatus(CustomerPrediction.Status.CONVERTED);
        prediction.setConvertedProjectId(projectId);
        CustomerPrediction saved = customerPredictionRepository.save(prediction);
        return CustomerOpportunityAssembler.toDTO(saved);
    }

    // ── private helpers ──────────────────────────────────────────

    private PredictionTransitionPolicy.PredictionStatus mapToCoreStatus(
            CustomerPrediction.Status entityStatus) {
        return switch (entityStatus) {
            case WATCH -> PredictionTransitionPolicy.PredictionStatus.WATCH;
            case RECOMMEND -> PredictionTransitionPolicy.PredictionStatus.RECOMMEND;
            case CONVERTED -> PredictionTransitionPolicy.PredictionStatus.CONVERTED;
            case CANCELLED -> PredictionTransitionPolicy.PredictionStatus.CANCELLED;
        };
    }

    private List<EnrichedTender> enrichTenders(List<Tender> tenders) {
        List<EnrichedTender> result = new ArrayList<>(tenders.size());
        for (Tender tender : tenders) {
            var extraction = PurchaserExtractionPolicy.extractPurchaser(tender.getTitle());
            String purchaserName = extraction.found() ? extraction.purchaserName() : "";
            String purchaserHash = extraction.found() ? extraction.purchaserHash() : "";
            String industry = IndustryClassificationPolicy.classifyIndustry(tender.getTitle());
            result.add(new EnrichedTender(tender, purchaserName, purchaserHash, industry));
        }
        return result;
    }

    private BigDecimal computeAvgBudgetYuan(List<EnrichedTender> group) {
        if (group.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (EnrichedTender et : group) {
            BigDecimal budget = et.tender().getBudget();
            if (budget != null) {
                total = total.add(budget);
            }
        }
        return total.divide(BigDecimal.valueOf(group.size()), 2, java.math.RoundingMode.HALF_UP);
    }

    private int computeMonthsSinceLast(List<EnrichedTender> group, LocalDateTime now) {
        LocalDateTime latest = null;
        for (EnrichedTender et : group) {
            LocalDateTime created = et.tender().getCreatedAt();
            if (created != null && (latest == null || created.isAfter(latest))) {
                latest = created;
            }
        }
        if (latest == null) {
            return 999;
        }
        long days = java.time.Duration.between(latest, now).toDays();
        return (int) Math.max(0L, days) / 30;
    }
}
