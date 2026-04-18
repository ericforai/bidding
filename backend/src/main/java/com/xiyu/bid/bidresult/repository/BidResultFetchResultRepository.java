package com.xiyu.bid.bidresult.repository;

import com.xiyu.bid.bidresult.entity.BidResultFetchResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BidResultFetchResultRepository extends JpaRepository<BidResultFetchResult, Long> {
    List<BidResultFetchResult> findByStatusOrderByFetchTimeDesc(BidResultFetchResult.Status status);
    long countByStatus(BidResultFetchResult.Status status);
    Optional<BidResultFetchResult> findFirstByTenderIdAndStatusOrderByFetchTimeDesc(Long tenderId, BidResultFetchResult.Status status);
    List<BidResultFetchResult> findByIdIn(Collection<Long> ids);
    List<BidResultFetchResult> findAllByOrderByFetchTimeDesc();
}
