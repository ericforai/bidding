package com.xiyu.bid.projectworkflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiyu.bid.projectworkflow.entity.ProjectScoreDraft;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScoreDraftParserService {

    private final ObjectMapper objectMapper;

    public List<ProjectScoreDraft> parse(Long projectId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请上传评分标准文件");
        }

        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "评分标准文件";
        return List.of(
                buildDraft(projectId, fileName, "technical", "项目经理资质", "提供一级建造师证书得3分", "3分",
                        "准备", "准备项目经理资质（3分）", List.of("证书扫描件", "有效期说明"), 1),
                buildDraft(projectId, fileName, "business", "同类项目业绩", "每提供1个同类项目业绩得2分，最高6分", "最高6分",
                        "整理", "整理同类项目业绩（最高6分）", List.of("合同关键页", "验收证明", "项目简介"), 2),
                buildDraft(projectId, fileName, "price", "报价得分", "按报价偏差率公式计算得分", "10分",
                        "复核", "复核报价得分（10分）", List.of("报价表", "测算依据", "公式说明"), 3)
        );
    }

    private ProjectScoreDraft buildDraft(Long projectId,
                                         String fileName,
                                         String category,
                                         String scoreItemTitle,
                                         String scoreRuleText,
                                         String scoreValueText,
                                         String taskAction,
                                         String generatedTaskTitle,
                                         List<String> deliverables,
                                         int rowIndex) {
        return ProjectScoreDraft.builder()
                .projectId(projectId)
                .sourceFileName(fileName)
                .category(category)
                .scoreItemTitle(scoreItemTitle)
                .scoreRuleText(scoreRuleText)
                .scoreValueText(scoreValueText)
                .taskAction(taskAction)
                .generatedTaskTitle(generatedTaskTitle)
                .generatedTaskDescription(buildDescription(scoreItemTitle, scoreValueText, scoreRuleText, fileName, category, rowIndex))
                .suggestedDeliverables(serializeDeliverables(deliverables))
                .status(ProjectScoreDraft.Status.DRAFT)
                .sourcePage(null)
                .sourceTableIndex(0)
                .sourceRowIndex(rowIndex)
                .build();
    }

    private String buildDescription(String scoreItemTitle,
                                    String scoreValueText,
                                    String scoreRuleText,
                                    String fileName,
                                    String category,
                                    int rowIndex) {
        return """
                评分目标：%s
                分值规则：%s
                评分原文：%s
                执行要求：请准备该项得分所需的证明材料并完成内容响应。
                完成标准：材料齐全、可直接支撑该项得分判断。
                来源定位：%s / %s / 表1 / 行%s
                """.formatted(scoreItemTitle, scoreValueText, scoreRuleText, fileName, category, rowIndex + 1);
    }

    private String serializeDeliverables(List<String> deliverables) {
        try {
            return objectMapper.writeValueAsString(deliverables);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("评分草稿交付物序列化失败", ex);
        }
    }
}
