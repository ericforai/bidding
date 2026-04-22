package com.xiyu.bid.biddraftagent.infrastructure.openai;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.StructuredResponse;
import com.openai.models.responses.StructuredResponseCreateParams;
import com.xiyu.bid.biddraftagent.application.TenderDocumentAnalysisInput;
import com.xiyu.bid.biddraftagent.application.TenderDocumentAnalyzer;
import com.xiyu.bid.biddraftagent.domain.TenderRequirementItemSnapshot;
import com.xiyu.bid.biddraftagent.domain.TenderRequirementProfile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
public class OpenAiTenderDocumentAnalyzer implements TenderDocumentAnalyzer {

    private static final int MAX_DOCUMENT_CHARS = 32000;

    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final Duration timeout;

    public OpenAiTenderDocumentAnalyzer(
            @Value("${ai.openai.api-key:}") String apiKey,
            @Value("${ai.openai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${ai.openai.model:gpt-5.2}") String model,
            @Value("${ai.openai.timeout:PT30S}") Duration timeout
    ) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        this.timeout = timeout;
    }

    @Override
    public TenderRequirementProfile analyze(TenderDocumentAnalysisInput input) {
        TenderRequirementOutput output = requestAnalysis(buildPrompt(input));
        return toProfile(output);
    }

    private TenderRequirementOutput requestAnalysis(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("ai.openai.api-key must be configured for tender document analysis");
        }
        StructuredResponseCreateParams<TenderRequirementOutput> params = ResponseCreateParams.builder()
                .input(prompt)
                .model(model)
                .text(TenderRequirementOutput.class)
                .build();
        StructuredResponse<TenderRequirementOutput> response = client().responses().create(params);
        return extractPayload(response)
                .orElseThrow(() -> new IllegalStateException("OpenAI structured response did not include tender requirements"));
    }

    private OpenAIClient client() {
        return OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .timeout(timeout)
                .build();
    }

    private Optional<TenderRequirementOutput> extractPayload(StructuredResponse<TenderRequirementOutput> response) {
        return response.output().stream()
                .map(item -> item.message())
                .flatMap(Optional::stream)
                .flatMap(message -> message.content().stream())
                .map(content -> content.outputText())
                .flatMap(Optional::stream)
                .findFirst();
    }

    private String buildPrompt(TenderDocumentAnalysisInput input) {
        return """
                你是招标文件解析 Agent。请读取招标文件正文，提取投标写作所需的结构化信息。
                必须遵守：
                - 只从正文中提取，无法确认的字段留空，不要编造。
                - requirementItems 必须逐条列出关键要求，至少覆盖资格、技术、商务、评分和材料清单中出现的要求。
                - category 只能使用 qualification、technical、commercial、pricing、legal、delivery、scoring、material、other。
                - mandatory 表示是否为必须响应/必须提供。
                - sourceExcerpt 保留能定位来源的短句，confidence 使用 0-100 整数。
                - 返回结构化字段 projectName、tenderTitle、tenderScope、purchaserName、qualificationRequirements、technicalRequirements、commercialRequirements、scoringCriteria、deadlineText、requiredMaterials、riskPoints、tags、requirementItems。

                项目ID: %s
                标讯ID: %s
                文件名: %s
                招标文件正文:
                %s
                """.formatted(
                input.projectId(),
                input.tenderId(),
                input.fileName(),
                truncate(input.extractedText())
        );
    }

    private String truncate(String text) {
        if (text == null) {
            return "";
        }
        if (text.length() <= MAX_DOCUMENT_CHARS) {
            return text;
        }
        return text.substring(0, MAX_DOCUMENT_CHARS) + "\n[后续内容因长度限制已截断，请在风险点中提示人工复核未解析尾部]";
    }

    private TenderRequirementProfile toProfile(TenderRequirementOutput output) {
        return new TenderRequirementProfile(
                output.projectName,
                output.tenderTitle,
                output.tenderScope,
                output.purchaserName,
                nullToList(output.qualificationRequirements),
                nullToList(output.technicalRequirements),
                nullToList(output.commercialRequirements),
                nullToList(output.scoringCriteria),
                output.deadlineText,
                nullToList(output.requiredMaterials),
                nullToList(output.riskPoints),
                nullToList(output.tags),
                nullToList(output.requirementItems).stream()
                        .map(this::toItem)
                        .toList()
        );
    }

    private TenderRequirementItemSnapshot toItem(TenderRequirementItemOutput item) {
        return new TenderRequirementItemSnapshot(
                item.category,
                item.title,
                item.content,
                item.mandatory,
                item.sourceExcerpt,
                item.confidence
        );
    }

    private <T> List<T> nullToList(List<T> values) {
        return values == null ? List.of() : values;
    }

    public static class TenderRequirementOutput {
        public String projectName;
        public String tenderTitle;
        public String tenderScope;
        public String purchaserName;
        public List<String> qualificationRequirements;
        public List<String> technicalRequirements;
        public List<String> commercialRequirements;
        public List<String> scoringCriteria;
        public String deadlineText;
        public List<String> requiredMaterials;
        public List<String> riskPoints;
        public List<String> tags;
        public List<TenderRequirementItemOutput> requirementItems;
    }

    public static class TenderRequirementItemOutput {
        public String category;
        public String title;
        public String content;
        public boolean mandatory;
        public String sourceExcerpt;
        public Integer confidence;
    }
}
