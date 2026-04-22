package com.xiyu.bid.biddraftagent.application;

import com.xiyu.bid.biddraftagent.domain.BidDraftSnapshot;
import com.xiyu.bid.biddraftagent.domain.GapCheckResult;
import com.xiyu.bid.biddraftagent.domain.ManualConfirmationDecision;
import com.xiyu.bid.biddraftagent.domain.MaterialMatchScore;
import com.xiyu.bid.biddraftagent.domain.RequirementClassification;
import com.xiyu.bid.biddraftagent.domain.WriteCoverageDecision;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

@Service
public class DeterministicBidDraftTextGenerator implements BidDraftTextGenerator {

    @Override
    public BidDraftGenerationResult generate(
            BidDraftSnapshot snapshot,
            RequirementClassification classification,
            MaterialMatchScore materialMatchScore,
            GapCheckResult gapCheck,
            ManualConfirmationDecision manualConfirmation,
            WriteCoverageDecision writeCoverage
    ) {
        String draftText = buildDraftText(snapshot, classification, materialMatchScore, gapCheck, manualConfirmation, writeCoverage);
        String reviewSummary = buildReviewSummary(materialMatchScore, gapCheck, manualConfirmation, writeCoverage);
        List<GeneratedArtifactSpec> artifactSpecs = List.of(
                new GeneratedArtifactSpec("DRAFT_TEXT", "自动生成投标草稿", draftText, "document-writer"),
                new GeneratedArtifactSpec("REVIEW_SUMMARY", "草稿审阅摘要", reviewSummary, "bid-reviewer"),
                new GeneratedArtifactSpec("HANDOFF_CHECKLIST", "文档写手交接清单", buildHandoffChecklist(gapCheck, manualConfirmation, writeCoverage), "document-writer")
        );
        return new BidDraftGenerationResult(draftText, reviewSummary, artifactSpecs);
    }

    private String buildDraftText(
            BidDraftSnapshot snapshot,
            RequirementClassification classification,
            MaterialMatchScore materialMatchScore,
            GapCheckResult gapCheck,
            ManualConfirmationDecision manualConfirmation,
            WriteCoverageDecision writeCoverage
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append("# 投标草稿\n\n");
        appendSection(builder, "项目概况", List.of(
                line("项目名称", snapshot.projectName()),
                line("标讯标题", snapshot.tenderTitle()),
                line("客户名称", snapshot.customerName()),
                line("区域", snapshot.region()),
                line("行业", snapshot.industry()),
                line("预算", snapshot.budget() == null ? null : snapshot.budget().toPlainString()),
                line("截止日期", snapshot.deadline() == null ? null : snapshot.deadline().toString())
        ));
        appendSection(builder, "需求分类", List.of(
                line("价格", joinOrNone(classification.pricingRequirements())),
                line("法务", joinOrNone(classification.legalRequirements())),
                line("资质", joinOrNone(classification.qualificationRequirements())),
                line("技术", joinOrNone(classification.technicalRequirements())),
                line("交付", joinOrNone(classification.deliveryRequirements())),
                line("商务", joinOrNone(classification.commercialRequirements()))
        ));
        appendSection(builder, "材料匹配", List.of(
                line("匹配类别", joinOrNone(materialMatchScore.matchedCategories())),
                line("缺失类别", joinOrNone(materialMatchScore.missingCategories())),
                line("覆盖得分", Integer.toString(materialMatchScore.score()))
        ));
        appendSection(builder, "人工确认", List.of(
                line("价格确认", manualConfirmation.pricingConfirmationRequired() ? "需要" : "不需要"),
                line("法务确认", manualConfirmation.legalConfirmationRequired() ? "需要" : "不需要"),
                line("资质真实性确认", manualConfirmation.qualificationAuthenticityConfirmationRequired() ? "需要" : "不需要"),
                line("确认原因", joinOrNone(manualConfirmation.reasons()))
        ));
        appendSection(builder, "写作覆盖", List.of(
                line("覆盖评分", Integer.toString(writeCoverage.coverageScore())),
                line("可写章节", joinOrNone(writeCoverage.coveredSections())),
                line("待补章节", joinOrNone(writeCoverage.missingSections())),
                line("建议章节", joinOrNone(writeCoverage.recommendedSections()))
        ));
        appendSection(builder, "建议正文", buildSuggestedParagraphs(snapshot, classification, gapCheck, writeCoverage));
        return builder.toString().trim();
    }

    private String buildReviewSummary(
            MaterialMatchScore materialMatchScore,
            GapCheckResult gapCheck,
            ManualConfirmationDecision manualConfirmation,
            WriteCoverageDecision writeCoverage
    ) {
        StringJoiner joiner = new StringJoiner("\n");
        joiner.add("覆盖评分: " + writeCoverage.coverageScore());
        joiner.add("材料匹配: " + materialMatchScore.score());
        joiner.add("可直接写作: " + (writeCoverage.sufficient() ? "是" : "否"));
        joiner.add("人工确认: " + (manualConfirmation.requiresConfirmation() ? "需要" : "不需要"));
        if (!gapCheck.gaps().isEmpty()) {
            joiner.add("缺口: " + String.join("; ", gapCheck.gaps()));
        }
        if (!gapCheck.suggestions().isEmpty()) {
            joiner.add("建议: " + String.join("; ", gapCheck.suggestions()));
        }
        return joiner.toString();
    }

    private String buildHandoffChecklist(
            GapCheckResult gapCheck,
            ManualConfirmationDecision manualConfirmation,
            WriteCoverageDecision writeCoverage
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append("交接目标: 文档写手\n");
        builder.append("可直接写作: ").append(writeCoverage.sufficient() ? "是" : "否").append('\n');
        builder.append("人工确认: ").append(manualConfirmation.requiresConfirmation() ? "需要" : "不需要").append('\n');
        builder.append("待处理缺口:\n");
        if (gapCheck.gaps().isEmpty()) {
            builder.append("- 无\n");
        } else {
            for (String gap : gapCheck.gaps()) {
                builder.append("- ").append(gap).append('\n');
            }
        }
        builder.append("建议动作:\n");
        if (gapCheck.suggestions().isEmpty()) {
            builder.append("- 无\n");
        } else {
            for (String suggestion : gapCheck.suggestions()) {
                builder.append("- ").append(suggestion).append('\n');
            }
        }
        return builder.toString().trim();
    }

    private List<String> buildSuggestedParagraphs(
            BidDraftSnapshot snapshot,
            RequirementClassification classification,
            GapCheckResult gapCheck,
            WriteCoverageDecision writeCoverage
    ) {
        List<String> paragraphs = new ArrayList<>();
        paragraphs.add("项目 " + safe(snapshot.projectName()) + " 的投标草稿已经根据标讯 " + safe(snapshot.tenderTitle()) + " 进行结构化整理。");
        if (classification.hasQualificationRequirement()) {
            paragraphs.add("资质部分优先引用已归集的资质信号，并在正式提交前完成真实性复核。");
        }
        if (classification.hasPricingRequirement()) {
            paragraphs.add("价格部分应保持与预算、限价和报价口径一致，避免与人工确认结论冲突。");
        }
        if (classification.hasLegalRequirement()) {
            paragraphs.add("法务部分建议保留合同条款、授权说明和合规声明的标准表达。");
        }
        if (classification.hasTechnicalRequirement() || classification.hasDeliveryRequirement()) {
            paragraphs.add("技术与交付部分应围绕实施方案、验收路径和售后支持进行展开。");
        }
        if (!gapCheck.gaps().isEmpty()) {
            paragraphs.add("当前仍存在材料缺口，建议先补齐: " + String.join("；", gapCheck.gaps()) + "。");
        }
        if (writeCoverage.sufficient()) {
            paragraphs.add("现有材料已足以进入正文写作阶段。");
        } else {
            paragraphs.add("现有材料还不足以直接定稿，建议先完成补充后再交给文档写手。");
        }
        return paragraphs;
    }

    private void appendSection(StringBuilder builder, String sectionTitle, List<String> lines) {
        builder.append("## ").append(sectionTitle).append('\n');
        for (String line : lines) {
            if (line != null && !line.isBlank()) {
                builder.append("- ").append(line).append('\n');
            }
        }
        builder.append('\n');
    }

    private String line(String label, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return label + ": " + value;
    }

    private String joinOrNone(List<String> values) {
        return values == null || values.isEmpty() ? "无" : String.join("、", values);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "未命名" : value;
    }
}
