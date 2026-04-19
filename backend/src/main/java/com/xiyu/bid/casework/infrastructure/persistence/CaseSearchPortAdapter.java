package com.xiyu.bid.casework.infrastructure.persistence;

import com.xiyu.bid.casework.domain.model.CaseSearchCriteria;
import com.xiyu.bid.casework.domain.port.CaseSearchPort;
import com.xiyu.bid.entity.Case;
import com.xiyu.bid.repository.CaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CaseSearchPortAdapter implements CaseSearchPort {

    private static final int SEARCH_POOL_SIZE = 2000;

    private final CaseRepository caseRepository;

    @Override
    public Page<Case> search(CaseSearchCriteria criteria, Pageable pageable) {
        Pageable fetchPageable = org.springframework.data.domain.PageRequest.of(0, Math.max(pageable.getPageSize(), SEARCH_POOL_SIZE), pageable.getSort());
        List<Case> candidates = caseRepository.findAll(fetchPageable).stream().toList();
        List<Case> filtered = candidates.stream()
                .filter(item -> matchesCriteria(item, criteria))
                .toList();

        int fromIndex = Math.min(pageable.getPageNumber() * pageable.getPageSize(), filtered.size());
        int toIndex = Math.min(fromIndex + pageable.getPageSize(), filtered.size());
        List<Case> pageContent = fromIndex >= toIndex ? List.of() : filtered.subList(fromIndex, toIndex);
        return new PageImpl<>(pageContent, pageable, filtered.size());
    }

    @Override
    public List<Case> findRelatedCandidates(Long excludedCaseId, Pageable pageable) {
        return caseRepository.findAll(pageable).stream()
                .filter(candidate -> !candidate.getId().equals(excludedCaseId))
                .toList();
    }

    @Override
    public List<String> findDistinctProductLines() {
        return caseRepository.findDistinctProductLines();
    }

    @Override
    public List<String> findDistinctStatuses() {
        return caseRepository.findDistinctStatuses();
    }

    @Override
    public List<String> findDistinctVisibilities() {
        return caseRepository.findDistinctVisibilities();
    }

    @Override
    public List<String> findDistinctTags() {
        return caseRepository.findAll().stream()
                .flatMap(item -> item.getTags() == null ? java.util.stream.Stream.<String>empty() : item.getTags().stream())
                .filter(this::hasText)
                .map(String::trim)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    @Override
    public Sort resolveSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("updatedAt"), Sort.Order.desc("id"));
        }

        return switch (sort.trim().toLowerCase(Locale.ROOT)) {
            case "popular" -> Sort.by(Sort.Order.desc("useCount"), Sort.Order.desc("viewCount"), Sort.Order.desc("id"));
            case "amountasc" -> Sort.by(Sort.Order.asc("amount"), Sort.Order.desc("id"));
            case "amountdesc" -> Sort.by(Sort.Order.desc("amount"), Sort.Order.desc("id"));
            case "oldest" -> Sort.by(Sort.Order.asc("publishedAt"), Sort.Order.asc("createdAt"), Sort.Order.asc("id"));
            default -> Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("updatedAt"), Sort.Order.desc("id"));
        };
    }

    private boolean matchesCriteria(Case item, CaseSearchCriteria criteria) {
        if (criteria == null) {
            return true;
        }
        if (hasText(criteria.keyword()) && !matchesKeyword(item, criteria.keyword())) {
            return false;
        }
        if (hasText(criteria.industry()) && (item.getIndustry() == null || !item.getIndustry().name().equalsIgnoreCase(criteria.industry()))) {
            return false;
        }
        if (hasText(criteria.productLine()) && !equalsIgnoreCase(item.getProductLine(), criteria.productLine())) {
            return false;
        }
        if (hasText(criteria.outcome()) && (item.getOutcome() == null || !item.getOutcome().name().equalsIgnoreCase(criteria.outcome()))) {
            return false;
        }
        if (criteria.year() != null && (item.getProjectDate() == null || item.getProjectDate().getYear() != criteria.year())) {
            return false;
        }
        if (criteria.amountMin() != null && (item.getAmount() == null || item.getAmount().compareTo(criteria.amountMin()) < 0)) {
            return false;
        }
        if (criteria.amountMax() != null && (item.getAmount() == null || item.getAmount().compareTo(criteria.amountMax()) > 0)) {
            return false;
        }
        if (criteria.tags() != null && !criteria.tags().isEmpty() && !hasAnyTagMatch(item.getTags(), criteria.tags())) {
            return false;
        }
        if (hasText(criteria.status()) && !equalsIgnoreCase(item.getStatus(), criteria.status())) {
            return false;
        }
        if (hasText(criteria.visibility()) && !equalsIgnoreCase(item.getVisibility(), criteria.visibility())) {
            return false;
        }
        return true;
    }

    private boolean matchesKeyword(Case item, String keyword) {
        String haystack = String.join(" ",
                safe(item.getTitle()),
                safe(item.getDescription()),
                safe(item.getCustomerName()),
                safe(item.getLocationName()),
                safe(item.getProjectPeriod()),
                safe(item.getProductLine()),
                safe(item.getArchiveSummary()),
                safe(item.getPriceStrategy()),
                safe(item.getDocumentSnapshotText()),
                safe(item.getSearchDocument()),
                join(item.getTags()),
                join(item.getHighlights()),
                join(item.getTechnologies()),
                join(item.getSuccessFactors()),
                join(item.getLessonsLearned()),
                join(item.getAttachmentNames())
        ).toLowerCase(Locale.ROOT);
        return haystack.contains(keyword.toLowerCase(Locale.ROOT));
    }

    private boolean hasAnyTagMatch(List<String> left, List<String> right) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) {
            return false;
        }
        Set<String> leftSet = left.stream().filter(this::hasText).map(this::normalize).collect(Collectors.toSet());
        Set<String> rightSet = right.stream().filter(this::hasText).map(this::normalize).collect(Collectors.toSet());
        leftSet.retainAll(rightSet);
        return !leftSet.isEmpty();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.trim().equalsIgnoreCase(right.trim());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return String.join(" ", values.stream().filter(this::hasText).map(String::trim).toList());
    }
}
