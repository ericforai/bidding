package com.xiyu.bid.bidresult.service;

import com.xiyu.bid.bidresult.dto.BidResultOverviewDTO;
import com.xiyu.bid.bidresult.dto.BidResultFetchResultDTO;
import com.xiyu.bid.bidresult.dto.BidResultReminderDTO;
import com.xiyu.bid.bidresult.dto.BidResultDetailDTO;
import com.xiyu.bid.bidresult.dto.BidResultAttachmentAssembler;
import com.xiyu.bid.bidresult.dto.BidResultFetchResultAssembler;
import com.xiyu.bid.bidresult.dto.BidResultReminderAssembler;
import com.xiyu.bid.bidresult.dto.CompetitorWinAssembler;
import com.xiyu.bid.bidresult.core.AttachmentRequirementResolver;
import com.xiyu.bid.bidresult.core.BidResultAttachmentRef;
import com.xiyu.bid.bidresult.entity.BidResultFetchResult;
import com.xiyu.bid.bidresult.entity.BidResultReminder;
import com.xiyu.bid.bidresult.repository.BidResultFetchResultRepository;
import com.xiyu.bid.bidresult.repository.BidResultReminderRepository;
import com.xiyu.bid.bidresult.repository.CompetitorWinRecordRepository;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.projectworkflow.entity.ProjectDocument;
import com.xiyu.bid.projectworkflow.repository.ProjectDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BidResultQueryService {

    private final BidResultFetchResultRepository fetchResultRepository;
    private final BidResultReminderRepository reminderRepository;
    private final CompetitorWinRecordRepository competitorWinRecordRepository;
    private final ProjectDocumentRepository projectDocumentRepository;

    public BidResultOverviewDTO getOverview() {
        long pendingFetch = fetchResultRepository.countByStatus(BidResultFetchResult.Status.PENDING);
        long pendingReminder = reminderRepository.countByStatus(BidResultReminder.ReminderStatus.PENDING);
        long competitorCount = competitorWinRecordRepository.countDistinctCompetitors();

        return BidResultOverviewDTO.builder()
                .pendingFetchCount(pendingFetch)
                .pendingReminderCount(pendingReminder)
                .competitorCount(competitorCount)
                .build();
    }

    public List<BidResultFetchResultDTO> getFetchResults() {
        return fetchResultRepository.findAllByOrderByFetchTimeDesc().stream()
                .map(BidResultFetchResultAssembler::toDto)
                .toList();
    }

    public List<BidResultReminderDTO> getReminders() {
        return reminderRepository.findAllByOrderByRemindTimeDesc().stream()
                .map(BidResultReminderAssembler::toDto)
                .toList();
    }

    public BidResultDetailDTO getDetail(Long id) {
        BidResultFetchResult entity = fetchResultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bid result not found: " + id));
        Map<Long, ProjectDocument> documents = projectDocumentRepository.findByProjectIdOrderByCreatedAtDesc(entity.getProjectId())
                .stream()
                .collect(Collectors.toMap(ProjectDocument::getId, Function.identity(), (left, right) -> left));
        BidResultReminder reminder = reminderRepository
                .findFirstByProjectIdAndReminderTypeOrderByRemindTimeDesc(
                        entity.getProjectId(),
                        entity.getResult() == BidResultFetchResult.Result.WON
                                ? BidResultReminder.ReminderType.NOTICE
                                : BidResultReminder.ReminderType.REPORT
                ).orElse(null);

        return BidResultDetailDTO.builder()
                .fetchResult(BidResultFetchResultAssembler.toDto(entity))
                .reminder(BidResultReminderAssembler.toDto(reminder))
                .requiredAttachment(BidResultAttachmentAssembler.required(
                        AttachmentRequirementResolver.requiredFor(entity.getResult())
                ))
                .noticeAttachment(resolveAttachment(
                        documents.get(entity.getNoticeDocumentId()),
                        entity.getWinAnnounceDocUrl(),
                        BidResultAttachmentRef.AttachmentType.NOTICE
                ))
                .analysisAttachment(resolveAttachment(
                        documents.get(entity.getAnalysisDocumentId()),
                        null,
                        BidResultAttachmentRef.AttachmentType.REPORT
                ))
                .competitorWins(competitorWinRecordRepository.findByProjectIdOrderByWonAtDesc(entity.getProjectId()).stream()
                        .map(CompetitorWinAssembler::toDto)
                        .toList())
                .build();
    }

    private com.xiyu.bid.bidresult.dto.BidResultAttachmentDTO resolveAttachment(
            ProjectDocument document,
            String legacyUrl,
            BidResultAttachmentRef.AttachmentType attachmentType
    ) {
        if (document != null) {
            return BidResultAttachmentAssembler.fromDocument(document, attachmentType);
        }
        return BidResultAttachmentAssembler.fromLegacyUrl(legacyUrl, attachmentType);
    }
}
