package com.xiyu.bid.tender.service;

import com.xiyu.bid.entity.Tender;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TenderSpecification {

    private TenderSpecification() {
    }

    public static Specification<Tender> byCriteria(TenderSearchCriteria criteria) {
        TenderSearchCriteria safeCriteria = criteria == null ? TenderSearchCriteria.empty() : criteria;
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (hasText(safeCriteria.getKeyword())) {
                String pattern = containsPattern(safeCriteria.getKeyword());
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("purchaserName")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("tags")), pattern)
                ));
            }

            if (safeCriteria.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), safeCriteria.getStatus()));
            }
            addStringEquals(predicates, criteriaBuilder, root.get("source"), safeCriteria.getSource());
            addStringEquals(predicates, criteriaBuilder, root.get("region"), safeCriteria.getRegion());
            addStringEquals(predicates, criteriaBuilder, root.get("industry"), safeCriteria.getIndustry());
            addStringContains(predicates, criteriaBuilder, root.get("purchaserName"), safeCriteria.getPurchaserName());
            addStringEquals(predicates, criteriaBuilder, root.get("purchaserHash"), safeCriteria.getPurchaserHash());

            if (safeCriteria.getBudgetMin() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("budget"), safeCriteria.getBudgetMin()));
            }
            if (safeCriteria.getBudgetMax() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("budget"), safeCriteria.getBudgetMax()));
            }
            if (safeCriteria.getDeadlineFrom() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("deadline"), safeCriteria.getDeadlineFrom()));
            }
            if (safeCriteria.getDeadlineTo() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("deadline"), safeCriteria.getDeadlineTo()));
            }
            if (safeCriteria.getPublishDateFrom() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("publishDate"), safeCriteria.getPublishDateFrom()));
            }
            if (safeCriteria.getPublishDateTo() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("publishDate"), safeCriteria.getPublishDateTo()));
            }
            if (safeCriteria.getAiScoreMin() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("aiScore"), safeCriteria.getAiScoreMin()));
            }
            if (safeCriteria.getAiScoreMax() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("aiScore"), safeCriteria.getAiScoreMax()));
            }

            if (query != null) {
                query.orderBy(
                        criteriaBuilder.desc(root.get("publishDate")),
                        criteriaBuilder.desc(root.get("createdAt"))
                );
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static void addStringEquals(
            List<Predicate> predicates,
            jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder,
            jakarta.persistence.criteria.Expression<String> path,
            String value
    ) {
        if (hasText(value)) {
            predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(path), normalize(value)));
        }
    }

    private static void addStringContains(
            List<Predicate> predicates,
            jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder,
            jakarta.persistence.criteria.Expression<String> path,
            String value
    ) {
        if (hasText(value)) {
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(path), containsPattern(value)));
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String containsPattern(String value) {
        return "%" + normalize(value) + "%";
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
