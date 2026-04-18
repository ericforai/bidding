package com.xiyu.bid.bidresult.service;

import com.xiyu.bid.bidresult.core.AwardRegistration;
import com.xiyu.bid.bidresult.core.AwardRegistrationValidation;
import com.xiyu.bid.bidresult.core.FunctionalResult;
import com.xiyu.bid.bidresult.dto.BidResultAssembler;
import com.xiyu.bid.bidresult.dto.BidResultFetchResultDTO;
import com.xiyu.bid.bidresult.dto.BidResultRegisterRequest;
import com.xiyu.bid.bidresult.dto.BidResultUpdateRequest;
import com.xiyu.bid.bidresult.entity.BidResultFetchResult;
import com.xiyu.bid.bidresult.repository.BidResultFetchResultRepository;
import com.xiyu.bid.entity.Project;
import com.xiyu.bid.exception.BusinessException;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 登记服务：手工登记 / 补录 / 确认抓取结果 / 忽略。
 * Split-First：只承载写入 + 调用 pure core 校验。
 */
@Service
@RequiredArgsConstructor
public class BidResultRegistrationService {

    private static final String MANUAL_SOURCE = "人工登记";

    private final BidResultFetchResultRepository fetchResultRepository;
    private final ProjectRepository projectRepository;
    private final BidResultReminderService reminderService;

    @Transactional
    public BidResultFetchResultDTO registerAward(BidResultRegisterRequest request, Long operatorId, String operatorName) {
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + request.getProjectId()));

        return parseResult(request.getResult())
                .map(outcome -> new AwardRegistration(
                        project.getId(),
                        project.getName(),
                        outcome,
                        request.getAmount(),
                        request.getContractStartDate(),
                        request.getContractEndDate(),
                        request.getContractDurationMonths(),
                        request.getRemark(),
                        request.getSkuCount(),
                        request.getWinAnnounceDocUrl()
                ))
                .flatMap(registration -> {
                    AwardRegistrationValidation.ValidationResult validation = AwardRegistrationValidation.validate(registration);
                    return validation.valid()
                            ? FunctionalResult.success(registration)
                            : FunctionalResult.failure(String.join("; ", validation.errors()));
                })
                .map(reg -> BidResultFetchResult.builder()
                        .source(MANUAL_SOURCE)
                        .tenderId(project.getTenderId())
                        .projectId(project.getId())
                        .projectName(project.getName())
                        .result(toEntityResult(reg.result()))
                        .amount(reg.amount())
                        .fetchTime(LocalDateTime.now())
                        .status(BidResultFetchResult.Status.CONFIRMED)
                        .confirmedAt(LocalDateTime.now())
                        .confirmedBy(operatorId)
                        .registrationType(BidResultFetchResult.RegistrationType.MANUAL)
                        .contractStartDate(reg.contractStartDate())
                        .contractEndDate(reg.contractEndDate())
                        .contractDurationMonths(reg.contractDurationMonths())
                        .remark(reg.remark())
                        .skuCount(reg.skuCount())
                        .winAnnounceDocUrl(reg.winAnnounceDocUrl())
                        .build())
                .map(fetchResultRepository::save)
                .map(saved -> {
                    reminderService.ensurePendingReminderForResult(saved, "结果已登记，请及时上传资料", operatorId, operatorName);
                    return BidResultAssembler.toFetchResultDTO(saved);
                })
                .orElseThrow(BusinessException::new);
    }

    @Transactional
    public BidResultFetchResultDTO updateAward(Long id, BidResultUpdateRequest request) {
        BidResultFetchResult current = getFetchResult(id);
        if (current.getStatus() == BidResultFetchResult.Status.IGNORED) {
            throw new BusinessException("已忽略的结果不可编辑，请先恢复为待确认");
        }

        BidResultFetchResult merged = current.toBuilder()
                .result(request.getResult() != null
                        ? toEntityResult(parseResult(request.getResult()).orElseThrow(BusinessException::new))
                        : current.getResult())
                .amount(request.getAmount() != null ? request.getAmount() : current.getAmount())
                .contractStartDate(request.getContractStartDate() != null ? request.getContractStartDate() : current.getContractStartDate())
                .contractEndDate(request.getContractEndDate() != null ? request.getContractEndDate() : current.getContractEndDate())
                .contractDurationMonths(request.getContractDurationMonths() != null ? request.getContractDurationMonths() : current.getContractDurationMonths())
                .remark(request.getRemark() != null ? request.getRemark() : current.getRemark())
                .skuCount(request.getSkuCount() != null ? request.getSkuCount() : current.getSkuCount())
                .winAnnounceDocUrl(request.getWinAnnounceDocUrl() != null ? request.getWinAnnounceDocUrl() : current.getWinAnnounceDocUrl())
                .build();

        AwardRegistration snapshot = new AwardRegistration(
                merged.getProjectId(),
                merged.getProjectName(),
                merged.getResult() == BidResultFetchResult.Result.WON
                        ? AwardRegistration.ResultOutcome.WON
                        : AwardRegistration.ResultOutcome.LOST,
                merged.getAmount(),
                merged.getContractStartDate(),
                merged.getContractEndDate(),
                merged.getContractDurationMonths(),
                merged.getRemark(),
                merged.getSkuCount(),
                merged.getWinAnnounceDocUrl()
        );
        AwardRegistrationValidation.ValidationResult validation = AwardRegistrationValidation.validate(snapshot);
        if (!validation.valid()) {
            throw new BusinessException(String.join("; ", validation.errors()));
        }

        return BidResultAssembler.toFetchResultDTO(fetchResultRepository.save(merged));
    }

    @Transactional
    public BidResultFetchResultDTO confirmFetchResult(Long id, Long userId, String operatorName) {
        BidResultFetchResult entity = getFetchResult(id);
        entity.setStatus(BidResultFetchResult.Status.CONFIRMED);
        entity.setConfirmedAt(LocalDateTime.now());
        entity.setConfirmedBy(userId);
        BidResultFetchResult saved = fetchResultRepository.save(entity);
        reminderService.ensurePendingReminderForResult(saved, "结果已确认，待上传中标资料", userId, operatorName);
        return BidResultAssembler.toFetchResultDTO(saved);
    }

    @Transactional
    public void ignoreFetchResult(Long id, String comment) {
        BidResultFetchResult entity = getFetchResult(id);
        entity.setStatus(BidResultFetchResult.Status.IGNORED);
        entity.setIgnoredReason(Optional.ofNullable(comment).filter(text -> !text.isBlank()).orElse("人工忽略"));
        fetchResultRepository.save(entity);
    }

    private static final int MAX_BATCH_SIZE = 200;

    @Transactional
    public int confirmBatch(List<Long> ids, Long userId, String operatorName) {
        List<Long> safeIds = Optional.ofNullable(ids).orElse(List.of());
        if (safeIds.size() > MAX_BATCH_SIZE) {
            throw new BusinessException("批量数量不得超过 " + MAX_BATCH_SIZE);
        }
        int count = 0;
        for (Long id : safeIds) {
            confirmFetchResult(id, userId, operatorName);
            count++;
        }
        return count;
    }

    private BidResultFetchResult getFetchResult(Long id) {
        return fetchResultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bid result fetch record not found: " + id));
    }

    private FunctionalResult<AwardRegistration.ResultOutcome, String> parseResult(String raw) {
        if (raw == null) {
            return FunctionalResult.success(null);
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "won" -> FunctionalResult.success(AwardRegistration.ResultOutcome.WON);
            case "lost" -> FunctionalResult.success(AwardRegistration.ResultOutcome.LOST);
            default -> FunctionalResult.failure("未知投标结果: " + raw);
        };
    }

    private BidResultFetchResult.Result toEntityResult(AwardRegistration.ResultOutcome outcome) {
        return outcome == AwardRegistration.ResultOutcome.WON
                ? BidResultFetchResult.Result.WON
                : BidResultFetchResult.Result.LOST;
    }
}
