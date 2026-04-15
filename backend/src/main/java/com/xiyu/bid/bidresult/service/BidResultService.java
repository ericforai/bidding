package com.xiyu.bid.bidresult.service;

import com.xiyu.bid.bidresult.dto.BidResultCompetitorReportRowDTO;
import com.xiyu.bid.bidresult.dto.BidResultDetailDTO;
import com.xiyu.bid.bidresult.dto.BidResultFetchResultDTO;
import com.xiyu.bid.bidresult.dto.BidResultOverviewDTO;
import com.xiyu.bid.bidresult.dto.BidResultReminderDTO;
import com.xiyu.bid.bidresult.dto.BidResultSyncResponseDTO;
import com.xiyu.bid.bidresult.entity.BidResultFetchResult;
import com.xiyu.bid.bidresult.entity.BidResultReminder;
import com.xiyu.bid.bidresult.entity.BidResultSyncLog;
import com.xiyu.bid.bidresult.repository.BidResultFetchResultRepository;
import com.xiyu.bid.bidresult.repository.BidResultReminderRepository;
import com.xiyu.bid.bidresult.repository.BidResultSyncLogRepository;
import com.xiyu.bid.competitionintel.dto.CompetitionAnalysisDTO;
import com.xiyu.bid.competitionintel.service.CompetitionIntelService;
import com.xiyu.bid.entity.Project;
import com.xiyu.bid.entity.Tender;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.repository.ProjectRepository;
import com.xiyu.bid.repository.TenderRepository;
import com.xiyu.bid.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BidResultService {

    private static final String INTERNAL_SYNC_SOURCE = "ERP/CRM";
    private static final String PUBLIC_FETCH_SOURCE = "公开信息抓取";

    private final BidResultFetchResultRepository fetchResultRepository;
    private final BidResultReminderRepository reminderRepository;
    private final BidResultSyncLogRepository syncLogRepository;
    private final TenderRepository tenderRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final CompetitionIntelService competitionIntelService;

    @Transactional(readOnly = true)
    public BidResultOverviewDTO getOverview() {
        LocalDateTime lastSyncTime = syncLogRepository
                .findFirstByOperationTypeOrderByCreatedAtDesc(BidResultSyncLog.OperationType.SYNC)
                .map(BidResultSyncLog::getCreatedAt)
                .orElse(null);

        return BidResultOverviewDTO.builder()
                .lastSyncTime(lastSyncTime)
                .pendingCount(fetchResultRepository.countByStatus(BidResultFetchResult.Status.PENDING))
                .uploadPending(reminderRepository.countByStatus(BidResultReminder.ReminderStatus.PENDING))
                .competitorCount(countCompetitorAnalyses())
                .build();
    }

    @Transactional
    public BidResultSyncResponseDTO syncInternal(Long operatorId, String operatorName) {
        int affected = upsertProjectDerivedResults(INTERNAL_SYNC_SOURCE, false);
        logSync(BidResultSyncLog.OperationType.SYNC, INTERNAL_SYNC_SOURCE, operatorId, operatorName, affected,
                affected == 0 ? "未发现新的结果记录" : "已同步内部项目结果");
        return BidResultSyncResponseDTO.builder()
                .affectedCount(affected)
                .message(affected == 0 ? "同步完成，未发现新的结果记录" : "同步完成，已更新结果记录")
                .build();
    }

    @Transactional
    public BidResultSyncResponseDTO fetchPublicResults(Long operatorId, String operatorName) {
        int affected = upsertProjectDerivedResults(PUBLIC_FETCH_SOURCE, true);
        logSync(BidResultSyncLog.OperationType.FETCH, PUBLIC_FETCH_SOURCE, operatorId, operatorName, affected,
                affected == 0 ? "未发现新的抓取结果" : "已生成待确认抓取结果");
        return BidResultSyncResponseDTO.builder()
                .affectedCount(affected)
                .message(affected == 0 ? "抓取完成，未发现新的待确认记录" : "抓取完成，已生成待确认记录")
                .build();
    }

    @Transactional(readOnly = true)
    public List<BidResultFetchResultDTO> getFetchResults() {
        return fetchResultRepository.findByStatusOrderByFetchTimeDesc(BidResultFetchResult.Status.PENDING).stream()
                .map(this::toFetchResultDTO)
                .toList();
    }

    @Transactional
    public BidResultFetchResultDTO confirmFetchResult(Long id, Long userId) {
        BidResultFetchResult entity = getFetchResult(id);
        entity.setStatus(BidResultFetchResult.Status.CONFIRMED);
        entity.setConfirmedAt(LocalDateTime.now());
        entity.setConfirmedBy(userId);
        BidResultFetchResult saved = fetchResultRepository.save(entity);
        ensurePendingReminderForResult(saved, "结果已确认，待上传中标资料", userId, "系统");
        return toFetchResultDTO(saved);
    }

    @Transactional
    public void ignoreFetchResult(Long id, String comment) {
        BidResultFetchResult entity = getFetchResult(id);
        entity.setStatus(BidResultFetchResult.Status.IGNORED);
        entity.setIgnoredReason(Optional.ofNullable(comment).filter(text -> !text.isBlank()).orElse("人工忽略"));
        fetchResultRepository.save(entity);
    }

    @Transactional
    public int confirmBatch(List<Long> ids, Long userId) {
        int count = 0;
        for (Long id : Optional.ofNullable(ids).orElse(List.of())) {
            confirmFetchResult(id, userId);
            count++;
        }
        return count;
    }

    @Transactional(readOnly = true)
    public List<BidResultReminderDTO> getReminders() {
        return reminderRepository.findAllByOrderByRemindTimeDesc().stream()
                .map(this::toReminderDTO)
                .toList();
    }

    @Transactional
    public BidResultReminderDTO sendReminder(Long resultId, String comment, Long operatorId, String operatorName) {
        BidResultFetchResult result = getFetchResult(resultId);
        BidResultReminder reminder = ensurePendingReminderForResult(result, comment, operatorId, operatorName);
        reminder.setStatus(BidResultReminder.ReminderStatus.REMINDED);
        reminder.setRemindTime(LocalDateTime.now());
        reminder.setLastReminderComment(Optional.ofNullable(comment).filter(text -> !text.isBlank()).orElse("已发送上传提醒"));
        reminder = reminderRepository.save(reminder);
        return toReminderDTO(reminder);
    }

    @Transactional
    public int sendReminders(List<Long> resultIds, String comment, Long operatorId, String operatorName) {
        int count = 0;
        for (Long resultId : Optional.ofNullable(resultIds).orElse(List.of())) {
            sendReminder(resultId, comment, operatorId, operatorName);
            count++;
        }
        return count;
    }

    @Transactional(readOnly = true)
    public BidResultDetailDTO getDetail(Long id) {
        BidResultFetchResult result = getFetchResult(id);
        List<String> reminderTypes = reminderRepository.findAllByOrderByRemindTimeDesc().stream()
                .filter(reminder -> Objects.equals(reminder.getProjectId(), result.getProjectId()))
                .map(reminder -> reminder.getReminderType().name())
                .distinct()
                .toList();

        return BidResultDetailDTO.builder()
                .id(result.getId())
                .source(result.getSource())
                .tenderId(result.getTenderId())
                .projectId(result.getProjectId())
                .projectName(result.getProjectName())
                .result(result.getResult().name().toLowerCase(Locale.ROOT))
                .amount(result.getAmount())
                .status(result.getStatus().name().toLowerCase(Locale.ROOT))
                .fetchTime(result.getFetchTime())
                .ignoredReason(result.getIgnoredReason())
                .ownerName(resolveOwnerName(result.getProjectId()))
                .reminderTypes(reminderTypes)
                .build();
    }

    @Transactional(readOnly = true)
    public List<BidResultCompetitorReportRowDTO> getCompetitorReport() {
        List<Project> projects = projectRepository.findAll();
        List<CompetitionAnalysisDTO> analyses = projects.stream()
                .flatMap(project -> competitionIntelService.getAnalysisByProject(project.getId()).stream())
                .toList();

        Map<Long, List<CompetitionAnalysisDTO>> byCompetitor = analyses.stream()
                .filter(item -> item.getCompetitorId() != null)
                .collect(Collectors.groupingBy(CompetitionAnalysisDTO::getCompetitorId));

        return byCompetitor.entrySet().stream()
                .map(entry -> {
                    Long competitorId = entry.getKey();
                    List<CompetitionAnalysisDTO> rows = entry.getValue();
                    double avgWinProbability = rows.stream()
                            .map(CompetitionAnalysisDTO::getWinProbability)
                            .filter(Objects::nonNull)
                            .mapToDouble(BigDecimal::doubleValue)
                            .average()
                            .orElse(0.0);
                    return BidResultCompetitorReportRowDTO.builder()
                            .company("竞争对手#" + competitorId)
                            .skuCount(String.valueOf(rows.size()))
                            .category("综合品类")
                            .discount("待补充")
                            .payment("待补充")
                            .winRate(formatPercent(avgWinProbability))
                            .projectCount(rows.size())
                            .trend(avgWinProbability >= 60 ? "up" : avgWinProbability >= 40 ? "flat" : "down")
                            .build();
                })
                .sorted(Comparator.comparingLong(BidResultCompetitorReportRowDTO::getProjectCount).reversed())
                .toList();
    }

    private int upsertProjectDerivedResults(String source, boolean pendingStatus) {
        List<Project> projects = projectRepository.findAll();
        List<Tender> tenders = tenderRepository.findAllById(projects.stream().map(Project::getTenderId).toList());
        Map<Long, Tender> tenderById = tenders.stream().collect(Collectors.toMap(Tender::getId, item -> item));

        int affected = 0;
        for (Project project : projects) {
            Tender tender = tenderById.get(project.getTenderId());
            if (tender == null) {
                continue;
            }
            Optional<BidResultFetchResult> existing = fetchResultRepository
                    .findFirstByTenderIdAndStatusOrderByFetchTimeDesc(tender.getId(), pendingStatus ? BidResultFetchResult.Status.PENDING : BidResultFetchResult.Status.CONFIRMED);

            if (existing.isPresent() && sameProject(existing.get(), project)) {
                continue;
            }

            fetchResultRepository.save(BidResultFetchResult.builder()
                    .source(source)
                    .tenderId(tender.getId())
                    .projectId(project.getId())
                    .projectName(project.getName())
                    .result(deriveResult(tender, project))
                    .amount(Optional.ofNullable(tender.getBudget()).orElse(BigDecimal.ZERO))
                    .fetchTime(LocalDateTime.now())
                    .status(pendingStatus ? BidResultFetchResult.Status.PENDING : BidResultFetchResult.Status.CONFIRMED)
                    .confirmedAt(pendingStatus ? null : LocalDateTime.now())
                    .build());
            affected++;
        }
        return affected;
    }

    private boolean sameProject(BidResultFetchResult existing, Project project) {
        return Objects.equals(existing.getProjectId(), project.getId())
                && Objects.equals(existing.getProjectName(), project.getName());
    }

    private BidResultFetchResult.Result deriveResult(Tender tender, Project project) {
        if (project.getStatus() == Project.Status.ARCHIVED) {
            return BidResultFetchResult.Result.WON;
        }
        if (tender.getStatus() == Tender.Status.ABANDONED) {
            return BidResultFetchResult.Result.LOST;
        }
        return project.getStatus() == Project.Status.BIDDING
                ? BidResultFetchResult.Result.WON
                : BidResultFetchResult.Result.LOST;
    }

    private void logSync(BidResultSyncLog.OperationType operationType, String source, Long operatorId, String operatorName,
                         int affectedCount, String message) {
        syncLogRepository.save(BidResultSyncLog.builder()
                .operationType(operationType)
                .source(source)
                .message(message)
                .affectedCount(affectedCount)
                .operatorId(operatorId)
                .operatorName(operatorName)
                .build());
    }

    private BidResultFetchResult getFetchResult(Long id) {
        return fetchResultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bid result fetch record not found: " + id));
    }

    private BidResultReminder ensurePendingReminderForResult(BidResultFetchResult result, String comment, Long operatorId, String operatorName) {
        BidResultReminder.ReminderType reminderType = result.getResult() == BidResultFetchResult.Result.WON
                ? BidResultReminder.ReminderType.NOTICE
                : BidResultReminder.ReminderType.REPORT;

        BidResultReminder reminder = reminderRepository
                .findFirstByProjectIdAndReminderTypeOrderByRemindTimeDesc(result.getProjectId(), reminderType)
                .orElse(BidResultReminder.builder()
                        .projectId(result.getProjectId())
                        .projectName(result.getProjectName())
                        .ownerId(resolveOwnerId(result.getProjectId()))
                        .ownerName(resolveOwnerName(result.getProjectId()))
                        .reminderType(reminderType)
                        .createdBy(operatorId)
                        .createdByName(operatorName)
                        .build());

        if (reminder.getStatus() == null || reminder.getStatus() != BidResultReminder.ReminderStatus.UPLOADED) {
            reminder.setStatus(BidResultReminder.ReminderStatus.PENDING);
        }
        reminder.setRemindTime(Optional.ofNullable(reminder.getRemindTime()).orElse(LocalDateTime.now()));
        reminder.setLastReminderComment(Optional.ofNullable(comment).filter(text -> !text.isBlank()).orElse("待上传中标资料"));
        reminder.setLastResultId(result.getId());
        if (reminder.getCreatedByName() == null || reminder.getCreatedByName().isBlank()) {
            reminder.setCreatedByName(operatorName);
        }
        return reminderRepository.save(reminder);
    }

    private Long resolveOwnerId(Long projectId) {
        return projectRepository.findById(projectId).map(Project::getManagerId).orElse(null);
    }

    private String resolveOwnerName(Long projectId) {
        return projectRepository.findById(projectId)
                .map(Project::getManagerId)
                .flatMap(userRepository::findById)
                .map(User::getFullName)
                .orElse("待分配");
    }

    private long countCompetitorAnalyses() {
        return projectRepository.findAll().stream()
                .map(Project::getId)
                .map(competitionIntelService::getAnalysisByProject)
                .mapToLong(List::size)
                .sum();
    }

    private BidResultFetchResultDTO toFetchResultDTO(BidResultFetchResult entity) {
        return BidResultFetchResultDTO.builder()
                .id(entity.getId())
                .source(entity.getSource())
                .tenderId(entity.getTenderId())
                .projectId(entity.getProjectId())
                .projectName(entity.getProjectName())
                .result(entity.getResult().name().toLowerCase(Locale.ROOT))
                .amount(entity.getAmount())
                .fetchTime(entity.getFetchTime())
                .status(entity.getStatus().name().toLowerCase(Locale.ROOT))
                .build();
    }

    private BidResultReminderDTO toReminderDTO(BidResultReminder entity) {
        return BidResultReminderDTO.builder()
                .id(entity.getId())
                .projectId(entity.getProjectId())
                .projectName(entity.getProjectName())
                .ownerId(entity.getOwnerId())
                .owner(entity.getOwnerName())
                .lastResultId(entity.getLastResultId())
                .type(entity.getReminderType().name().toLowerCase(Locale.ROOT))
                .status(entity.getStatus().name().toLowerCase(Locale.ROOT))
                .remindTime(entity.getRemindTime())
                .lastReminderComment(entity.getLastReminderComment())
                .build();
    }

    private String formatPercent(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP) + "%";
    }
}
