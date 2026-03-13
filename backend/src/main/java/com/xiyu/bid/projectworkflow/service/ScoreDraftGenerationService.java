package com.xiyu.bid.projectworkflow.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ScoreDraftGenerationService {

    public String detectCategory(String headerText) {
        String normalized = normalize(headerText);
        if (normalized.contains("技术")) {
            return "technical";
        }
        if (normalized.contains("商务")) {
            return "business";
        }
        if (normalized.contains("价格") || normalized.contains("报价")) {
            return "price";
        }
        return "other";
    }

    public String detectAction(String title, String ruleText) {
        String normalized = normalize(title + " " + ruleText);
        if (containsAny(normalized, "业绩", "案例", "合同", "项目经验")) {
            return "整理";
        }
        if (containsAny(normalized, "提供", "具备", "拥有", "获得", "资质", "证书", "认证")) {
            return "准备";
        }
        if (containsAny(normalized, "方案", "响应", "说明", "承诺")) {
            return "编写";
        }
        if (containsAny(normalized, "报价", "测算", "公式", "偏差率", "价格")) {
            return "复核";
        }
        return "处理";
    }

    public String generateTaskTitle(String action, String title, String scoreValueText) {
        String compactTitle = fallback(title, "评分响应");
        String compactScore = fallback(scoreValueText, "").trim();
        if (!compactScore.isBlank()) {
            return action + compactTitle + "（" + compactScore + "）";
        }
        return action + compactTitle;
    }

    public String generateTaskDescription(String sourceFileName,
                                          String category,
                                          String title,
                                          String ruleText,
                                          String scoreValueText,
                                          Integer sourceTableIndex,
                                          Integer sourceRowIndex) {
        StringBuilder builder = new StringBuilder();
        builder.append("评分目标：").append(fallback(title, "未命名评分项")).append("\n");
        if (!fallback(scoreValueText, "").isBlank()) {
            builder.append("分值规则：").append(scoreValueText.trim()).append("\n");
        }
        builder.append("评分原文：").append(fallback(ruleText, "无")).append("\n");
        builder.append("执行要求：请围绕该评分点准备支撑材料并补充响应内容，确保该项具备得分依据。\n");
        builder.append("完成标准：材料齐全、可复核、可直接支撑该评分项得分判断。\n");
        builder.append("来源定位：")
                .append(sourceFileName)
                .append(" / ")
                .append(category)
                .append(" / 表")
                .append(sourceTableIndex == null ? 0 : sourceTableIndex + 1)
                .append(" / 行")
                .append(sourceRowIndex == null ? 0 : sourceRowIndex + 1);
        return builder.toString();
    }

    public List<String> suggestDeliverables(String title, String ruleText, String category) {
        String normalized = normalize(title + " " + ruleText + " " + category);
        Set<String> deliverables = new LinkedHashSet<>();
        if (containsAny(normalized, "资质", "证书", "认证", "许可证")) {
            deliverables.add("证书扫描件");
            deliverables.add("有效期说明");
        }
        if (containsAny(normalized, "业绩", "案例", "合同", "验收")) {
            deliverables.add("合同关键页");
            deliverables.add("验收证明");
            deliverables.add("项目简介");
        }
        if (containsAny(normalized, "人员", "项目经理", "团队", "简历", "社保")) {
            deliverables.add("人员简历");
            deliverables.add("证书材料");
            deliverables.add("社保或劳动关系证明");
        }
        if (containsAny(normalized, "方案", "技术", "响应", "实施", "服务")) {
            deliverables.add("响应方案正文");
            deliverables.add("实施计划");
        }
        if (containsAny(normalized, "价格", "报价", "测算", "公式", "偏差率")) {
            deliverables.add("报价表");
            deliverables.add("测算依据");
            deliverables.add("公式说明");
        }
        if (deliverables.isEmpty()) {
            deliverables.add("支撑材料");
        }
        return new ArrayList<>(deliverables);
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return fallback(value, "").toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private String fallback(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }
}
