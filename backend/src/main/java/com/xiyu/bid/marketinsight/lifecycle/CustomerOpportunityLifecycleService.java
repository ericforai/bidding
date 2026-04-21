package com.xiyu.bid.marketinsight.lifecycle;

import com.xiyu.bid.entity.Tender;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.marketinsight.core.IndustryClassificationPolicy;
import com.xiyu.bid.marketinsight.core.OpportunityScoringPolicy;
import com.xiyu.bid.marketinsight.core.PredictionTransitionPolicy;
import com.xiyu.bid.marketinsight.core.PurchaserExtractionPolicy;
import com.xiyu.bid.marketinsight.dto.CustomerOpportunityAssembler;
import com.xiyu.bid.marketinsight.dto.CustomerPredictionDTO;
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
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 客户商机生命周期服务。
 * 负责刷新、状态流转和转项目回写。
 */
@Service
@RequiredArgsConstructor
public class CustomerOpportunityLifecycleService {

    private final TenderRepository tenderRepository;
    private final CustomerPredictionRepository customerPredictionRepository;

    record EnrichedTender(Tender tender, String purchaserName, String purchaserHash, String industry) {
    }

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

    @Transactional
    public CustomerPredictionDTO transitionPrediction(Long id, String targetStatus) {
        CustomerPrediction prediction = customerPredictionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CustomerPrediction", String.valueOf(id)));

        PredictionTransitionPolicy.PredictionStatus coreCurrent = mapToCoreStatus(prediction.getStatus());
        PredictionTransitionPolicy.PredictionStatus coreTarget = PredictionTransitionPolicy.PredictionStatus.valueOf(targetStatus);

        var result = PredictionTransitionPolicy.validateTransition(coreCurrent, coreTarget);
        if (!result.allowed()) {
            throw new IllegalStateException(result.reason());
        }

        prediction.setStatus(CustomerPrediction.Status.valueOf(targetStatus));
        CustomerPrediction saved = customerPredictionRepository.save(prediction);
        return CustomerOpportunityAssembler.toDTO(saved);
    }

    @Transactional
    public CustomerPredictionDTO convertPrediction(Long id, Long projectId) {
        CustomerPrediction prediction = customerPredictionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CustomerPrediction", String.valueOf(id)));

        Long resolvedProjectId = projectId != null ? projectId : prediction.getConvertedProjectId();
        boolean alreadyConverted = prediction.getStatus() == CustomerPrediction.Status.CONVERTED;
        boolean projectIdUnchanged = Objects.equals(prediction.getConvertedProjectId(), resolvedProjectId);

        if (alreadyConverted && projectIdUnchanged) {
            return CustomerOpportunityAssembler.toDTO(prediction);
        }

        PredictionTransitionPolicy.PredictionStatus coreCurrent = mapToCoreStatus(prediction.getStatus());
        var result = PredictionTransitionPolicy.validateTransition(
                coreCurrent, PredictionTransitionPolicy.PredictionStatus.CONVERTED);
        if (!result.allowed()) {
            throw new IllegalStateException(result.reason());
        }

        prediction.setStatus(CustomerPrediction.Status.CONVERTED);
        if (resolvedProjectId != null) {
            prediction.setConvertedProjectId(resolvedProjectId);
        }
        CustomerPrediction saved = customerPredictionRepository.save(prediction);
        return CustomerOpportunityAssembler.toDTO(saved);
    }

    private PredictionTransitionPolicy.PredictionStatus mapToCoreStatus(CustomerPrediction.Status entityStatus) {
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

    private CustomerPrediction buildPrediction(String hash, List<EnrichedTender> group, LocalDateTime now) {
        String name = group.get(0).purchaserName();
        String industry = group.get(0).industry();
        int frequency = group.size();
        BigDecimal avgBudgetYuan = computeAvgBudgetYuan(group);
        long avgBudgetWan = avgBudgetYuan.divide(BigDecimal.valueOf(10_000L), 0, java.math.RoundingMode.HALF_UP).longValue();
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
        String evidenceIds = group.stream().map(et -> String.valueOf(et.tender().getId())).collect(Collectors.joining(","));
        String mainCategories = group.stream().map(EnrichedTender::industry).distinct().collect(Collectors.joining(","));
        String periodMonths = monthCounts.keySet().stream().sorted().map(String::valueOf).collect(Collectors.joining(","));
        BigDecimal predictedMin = avgBudgetYuan.multiply(BigDecimal.valueOf(0.8));
        BigDecimal predictedMax = avgBudgetYuan.multiply(BigDecimal.valueOf(1.2));
        String reasoning = OpportunityScoringPolicy.generateReasoningSummary(name, industry, frequency, industry, window.confidence());

        CustomerPrediction prediction = customerPredictionRepository.findByPurchaserHash(hash)
                .stream().findFirst()
                .orElseGet(() -> CustomerPrediction.builder().purchaserHash(hash).purchaserName(name).build());

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
