package com.xiyu.bid.tender.service;

import com.xiyu.bid.entity.Tender;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
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
                predicates.add(criteriaBuilder.like(root.get("searchTextNormalized"), pattern));
            }

            if (safeCriteria.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), safeCriteria.getStatus()));
            }
            addStringEquals(predicates, criteriaBuilder, root.get("sourceNormalized"), safeCriteria.getSource());
            addStringEquals(predicates, criteriaBuilder, root.get("regionNormalized"), safeCriteria.getRegion());
            addStringEquals(predicates, criteriaBuilder, root.get("industryNormalized"), safeCriteria.getIndustry());
            addStringContains(
                    predicates,
                    criteriaBuilder,
                    root.get("purchaserNameNormalized"),
                    safeCriteria.getPurchaserName()
            );
            addStringEquals(
                    predicates,
                    criteriaBuilder,
                    root.get("purchaserHashNormalized"),
                    safeCriteria.getPurchaserHash()
            );

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
            CriteriaBuilder criteriaBuilder,
            Expression<String> path,
            String value
    ) {
        if (hasText(value)) {
            predicates.add(criteriaBuilder.equal(path, normalize(value)));
        }
    }

    private static void addStringContains(
            List<Predicate> predicates,
            CriteriaBuilder criteriaBuilder,
            Expression<String> path,
            String value
    ) {
        if (hasText(value)) {
            predicates.add(criteriaBuilder.like(path, containsPattern(value)));
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
