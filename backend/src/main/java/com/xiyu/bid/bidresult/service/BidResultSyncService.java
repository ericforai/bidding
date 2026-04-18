package com.xiyu.bid.bidresult.service;

import com.xiyu.bid.bidresult.dto.BidResultSyncResponseDTO;
import com.xiyu.bid.bidresult.repository.BidResultFetchResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BidResultSyncService {

    private final BidResultFetchResultRepository fetchResultRepository;

    @Transactional
    public BidResultSyncResponseDTO syncInternal(Long userId, String userName) {
        // Mock implementation for sync logic
        return BidResultSyncResponseDTO.builder()
                .affectedCount(0)
                .message("已同步内部 ERP/CRM 数据 (Mock)")
                .build();
    }

    @Transactional
    public BidResultSyncResponseDTO fetchPublicResults(Long userId, String userName) {
        // Mock implementation for fetch logic
        return BidResultSyncResponseDTO.builder()
                .affectedCount(0)
                .message("已完成公开投标信息抓取 (Mock)")
                .build();
    }
}
