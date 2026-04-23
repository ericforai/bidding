package com.xiyu.bid.biddraftagent.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record TenderRequirementProfile(
        String projectName,
        String tenderTitle,
        String tenderScope,
        String purchaserName,
        List<String> qualificationRequirements,
        List<String> technicalRequirements,
        List<String> commercialRequirements,
        List<String> scoringCriteria,
        String deadlineText,
        List<String> requiredMaterials,
        List<String> riskPoints,
        List<String> tags,
        List<TenderRequirementItemSnapshot> items
) {

    public TenderRequirementProfile {
        qualificationRequirements = normalizeStrings(qualificationRequirements);
        technicalRequirements = normalizeStrings(technicalRequirements);
        commercialRequirements = normalizeStrings(commercialRequirements);
        scoringCriteria = normalizeStrings(scoringCriteria);
        requiredMaterials = normalizeStrings(requiredMaterials);
        riskPoints = normalizeStrings(riskPoints);
        tags = normalizeStrings(tags);
        items = normalizeItems(items);
    }

    public List<String> requirementSignals() {
        List<String> signals = new ArrayList<>();
        signals.addAll(qualificationRequirements);
        signals.addAll(technicalRequirements);
        signals.addAll(commercialRequirements);
        items.stream()
                .map(TenderRequirementProfile::formatItemSignal)
                .filter(signal -> !signal.isBlank())
                .forEach(signals::add);
        return List.copyOf(signals);
    }

    private static String formatItemSignal(TenderRequirementItemSnapshot item) {
        String title = blankToEmpty(item.title());
        String content = blankToEmpty(item.content());
        String category = blankToEmpty(item.category());
        return (category + " / " + title + " / " + content).trim();
    }

    private static List<String> normalizeStrings(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private static List<TenderRequirementItemSnapshot> normalizeItems(List<TenderRequirementItemSnapshot> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .toList();
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
