package com.xiyu.bid.tender.service;

import com.xiyu.bid.entity.Tender;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.repository.TenderRepository;
import com.xiyu.bid.tender.dto.TenderDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TenderQueryService {

    private final TenderRepository tenderRepository;
    private final TenderMapper tenderMapper;
    private final TenderProjectAccessGuard accessGuard;

    public List<TenderDTO> searchTenders(TenderSearchCriteria criteria) {
        log.debug("Searching tenders with criteria: {}", criteria);
        return accessGuard.filterVisibleTenders(tenderRepository.findAll(TenderSpecification.byCriteria(criteria))).stream()
                .map(tenderMapper::toDTO)
                .toList();
    }

    public TenderDTO getTenderById(Long id) {
        log.debug("Fetching tender by id: {}", id);
        Tender tender = tenderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tender", id.toString()));
        accessGuard.assertCanAccessTender(tender);
        return tenderMapper.toDTO(tender);
    }

    public List<TenderDTO> getTendersByStatus(Tender.Status status) {
        log.debug("Fetching tenders by status: {}", status);
        return accessGuard.filterVisibleTenders(tenderRepository.findByStatus(status)).stream()
                .map(tenderMapper::toDTO)
                .toList();
    }

    public List<TenderDTO> getTendersBySource(String source) {
        log.debug("Fetching tenders by source: {}", source);
        return accessGuard.filterVisibleTenders(tenderRepository.findBySource(source)).stream()
                .map(tenderMapper::toDTO)
                .toList();
    }

    public Map<Tender.Status, Long> getTenderStatistics() {
        log.debug("Fetching tender statistics");
        List<Tender> visibleTenders = accessGuard.filterVisibleTenders(tenderRepository.findAll());
        return Map.of(
                Tender.Status.PENDING, countStatus(visibleTenders, Tender.Status.PENDING),
                Tender.Status.TRACKING, countStatus(visibleTenders, Tender.Status.TRACKING),
                Tender.Status.BIDDED, countStatus(visibleTenders, Tender.Status.BIDDED),
                Tender.Status.ABANDONED, countStatus(visibleTenders, Tender.Status.ABANDONED)
        );
    }

    private long countStatus(List<Tender> tenders, Tender.Status status) {
        return tenders.stream().filter(tender -> tender.getStatus() == status).count();
    }
}
