package com.xiyu.bid.biddraftagent.application;

import com.xiyu.bid.biddraftagent.domain.TenderRequirementProfile;
import com.xiyu.bid.entity.Tender;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
class TenderRequirementSnapshotUpdater {

    void apply(Tender tender, TenderRequirementProfile profile) {
        if (profile.tenderTitle() != null && !profile.tenderTitle().isBlank()) {
            tender.setTitle(profile.tenderTitle().trim());
        }
        if (profile.purchaserName() != null && !profile.purchaserName().isBlank()) {
            tender.setPurchaserName(profile.purchaserName().trim());
        }
        tender.setDescription(buildDescription(profile));
        tender.setTags(mergeTags(tender.getTags(), profile.tags()));
    }

    private String buildDescription(TenderRequirementProfile profile) {
        StringBuilder description = new StringBuilder();
        appendLine(description, "招标范围", profile.tenderScope());
        appendList(description, "资格要求", profile.qualificationRequirements());
        appendList(description, "技术要求", profile.technicalRequirements());
        appendList(description, "商务要求", profile.commercialRequirements());
        appendList(description, "评分标准", profile.scoringCriteria());
        appendLine(description, "截止时间", profile.deadlineText());
        appendList(description, "必须提供的材料", profile.requiredMaterials());
        appendList(description, "风险点", profile.riskPoints());
        return description.toString().trim();
    }

    private void appendLine(StringBuilder target, String label, String value) {
        if (value != null && !value.isBlank()) {
            target.append(label).append("：").append(value.trim()).append('\n');
        }
    }

    private void appendList(StringBuilder target, String label, List<String> values) {
        if (values != null && !values.isEmpty()) {
            target.append(label).append("：").append(String.join("；", values)).append('\n');
        }
    }

    private String mergeTags(String existingTags, List<String> extractedTags) {
        Set<String> tags = new LinkedHashSet<>();
        if (existingTags != null && !existingTags.isBlank()) {
            for (String tag : existingTags.split(",")) {
                addTag(tags, tag);
            }
        }
        if (extractedTags != null) {
            extractedTags.forEach(tag -> addTag(tags, tag));
        }
        return String.join(",", tags);
    }

    private void addTag(Set<String> tags, String tag) {
        if (tag != null && !tag.isBlank()) {
            tags.add(tag.trim());
        }
    }
}
