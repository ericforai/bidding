package com.xiyu.bid.bidresult.service;

import com.xiyu.bid.bidresult.dto.BidResultOverviewDTO;
import com.xiyu.bid.bidresult.dto.BidResultFetchResultDTO;
import com.xiyu.bid.bidresult.dto.BidResultReminderDTO;
import com.xiyu.bid.bidresult.dto.BidResultDetailDTO;
import com.xiyu.bid.bidresult.dto.BidResultAssembler;
import com.xiyu.bid.bidresult.entity.BidResultFetchResult;
import com.xiyu.bid.bidresult.entity.BidResultReminder;
import com.xiyu.bid.bidresult.repository.BidResultFetchResultRepository;
import com.xiyu.bid.bidresult.repository.BidResultReminderRepository;
import com.xiyu.bid.bidresult.repository.CompetitorWinRecordRepository;
import com.xiyu.bid.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BidResultQueryService {

    private final BidResultFetchResultRepository fetchResultRepository;
    private final BidResultReminderRepository reminderRepository;
    private final CompetitorWinRecordRepository competitorWinRecordRepository;

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
                .map(BidResultAssembler::toFetchResultDTO)
                .toList();
    }

    public List<BidResultReminderDTO> getReminders() {
        return reminderRepository.findAllByOrderByRemindTimeDesc().stream()
                .map(BidResultAssembler::toReminderDTO)
                .toList();
    }

    public BidResultDetailDTO getDetail(Long id) {
        BidResultFetchResult entity = fetchResultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bid result not found: " + id));
        
        return BidResultDetailDTO.builder()
                .fetchResult(BidResultAssembler.toFetchResultDTO(entity))
                .build();
    }
}
