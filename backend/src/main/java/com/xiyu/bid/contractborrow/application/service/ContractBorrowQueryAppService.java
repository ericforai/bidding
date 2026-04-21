package com.xiyu.bid.contractborrow.application.service;

import com.xiyu.bid.contractborrow.application.command.ContractBorrowQueryCriteria;
import com.xiyu.bid.contractborrow.application.view.ContractBorrowEventView;
import com.xiyu.bid.contractborrow.application.view.ContractBorrowOverviewView;
import com.xiyu.bid.contractborrow.application.view.ContractBorrowView;
import com.xiyu.bid.contractborrow.domain.valueobject.ContractBorrowStatus;
import com.xiyu.bid.contractborrow.infrastructure.persistence.entity.ContractBorrowApplicationEntity;
import com.xiyu.bid.contractborrow.infrastructure.persistence.repository.ContractBorrowApplicationJpaRepository;
import com.xiyu.bid.contractborrow.infrastructure.persistence.repository.ContractBorrowEventJpaRepository;
import com.xiyu.bid.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ContractBorrowQueryAppService {

    private final ContractBorrowApplicationJpaRepository applicationRepository;
    private final ContractBorrowEventJpaRepository eventRepository;
    private final ContractBorrowMapper mapper;

    @Transactional(readOnly = true)
    public ContractBorrowOverviewView overview() {
        LocalDate today = LocalDate.now();
        List<ContractBorrowApplicationEntity> applications = applicationRepository.findAll();
        long overdue = applications.stream()
            .filter(application -> mapper.toDomain(application).isOverdue(today))
            .count();
        return new ContractBorrowOverviewView(
            applications.size(),
            applicationRepository.countByStatus(ContractBorrowStatus.PENDING_APPROVAL),
            applicationRepository.countByStatus(ContractBorrowStatus.APPROVED),
            applicationRepository.countByStatus(ContractBorrowStatus.BORROWED),
            applicationRepository.countByStatus(ContractBorrowStatus.RETURNED),
            applicationRepository.countByStatus(ContractBorrowStatus.REJECTED),
            applicationRepository.countByStatus(ContractBorrowStatus.CANCELLED),
            overdue
        );
    }

    @Transactional(readOnly = true)
    public List<ContractBorrowView> list(ContractBorrowQueryCriteria criteria) {
        LocalDate today = LocalDate.now();
        return applicationRepository.findByOrderBySubmittedAtDesc().stream()
            .map(application -> mapper.toView(application, today))
            .filter(view -> matchesStatus(view, criteria.status()))
            .filter(view -> containsKeyword(view, criteria.keyword()))
            .filter(view -> sameText(view.borrowerName(), criteria.borrowerName()))
            .toList();
    }

    @Transactional(readOnly = true)
    public ContractBorrowView detail(Long id) {
        ContractBorrowApplicationEntity entity = applicationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ContractBorrowApplication", String.valueOf(id)));
        return mapper.toView(entity, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<ContractBorrowEventView> events(Long applicationId) {
        if (!applicationRepository.existsById(applicationId)) {
            throw new ResourceNotFoundException("ContractBorrowApplication", String.valueOf(applicationId));
        }
        return eventRepository.findByApplicationIdOrderByCreatedAtAsc(applicationId).stream()
            .map(mapper::toEventView)
            .toList();
    }

    private boolean matchesStatus(ContractBorrowView view, String status) {
        if (status == null || status.isBlank()) {
            return true;
        }
        String expected = status.trim().toUpperCase(Locale.ROOT);
        if ("OVERDUE".equals(expected)) {
            return view.overdue();
        }
        return view.status().name().equals(expected);
    }

    private boolean containsKeyword(ContractBorrowView view, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String normalized = keyword.trim().toLowerCase(Locale.ROOT);
        return contains(view.contractNo(), normalized)
            || contains(view.contractName(), normalized)
            || contains(view.customerName(), normalized)
            || contains(view.borrowerName(), normalized);
    }

    private boolean sameText(String actual, String expected) {
        return expected == null
            || expected.isBlank()
            || String.valueOf(actual).equalsIgnoreCase(expected.trim());
    }

    private boolean contains(String actual, String normalizedKeyword) {
        return actual != null && actual.toLowerCase(Locale.ROOT).contains(normalizedKeyword);
    }
}
