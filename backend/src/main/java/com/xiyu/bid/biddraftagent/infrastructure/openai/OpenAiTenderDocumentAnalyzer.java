package com.xiyu.bid.biddraftagent.infrastructure.openai;

import com.xiyu.bid.biddraftagent.application.TenderDocumentAnalysisInput;
import com.xiyu.bid.biddraftagent.application.TenderDocumentAnalyzer;
import com.xiyu.bid.biddraftagent.domain.TenderRequirementItemSnapshot;
import com.xiyu.bid.biddraftagent.domain.TenderRequirementProfile;
import com.xiyu.bid.docinsight.application.DocumentAnalysisInput;
import com.xiyu.bid.docinsight.application.DocumentAnalysisResult;
import com.xiyu.bid.docinsight.domain.DocumentChunk;
import com.xiyu.bid.docinsight.domain.StructuralDocumentChunker;
import com.xiyu.bid.docinsight.infrastructure.openai.BaseOpenAiDocumentAnalyzer;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OpenAiTenderDocumentAnalyzer extends BaseOpenAiDocumentAnalyzer<OpenAiTenderDocumentAnalyzer.TenderRequirementOutput> implements TenderDocumentAnalyzer {

    private static final String PROFILE_CODE = "TENDER";
    private static final String USE_CASE = "tender document analysis";
    private static final BigDecimal WAN_UNIT = new BigDecimal("10000");
    private static final Pattern BUDGET_NUMBER = Pattern.compile("^(\\d+(?:\\.\\d{1,2})?)(?:\\s*(万元|万|元))?$");
    private static final Pattern DATE_VALUE = Pattern.compile("^(\\d{4})-(\\d{2})-(\\d{2})$");
    private static final Pattern DATE_TIME_VALUE = Pattern.compile("^(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2}):(\\d{2}):(\\d{2})$");

    private final OpenAiBidAgentConfigurationResolver configurationResolver;
    private final OpenAiStructuredOutputService structuredOutputService;

    public OpenAiTenderDocumentAnalyzer(
            OpenAiBidAgentConfigurationResolver pConfigurationResolver,
            OpenAiStructuredOutputService pStructuredOutputService,
            StructuralDocumentChunker pStructuralChunker
    ) {
        super(pStructuralChunker);
        this.configurationResolver = pConfigurationResolver;
        this.structuredOutputService = pStructuredOutputService;
    }

    @Override
    public boolean supports(String profileCode) {
        return PROFILE_CODE.equalsIgnoreCase(profileCode);
    }

    @Override
    public TenderRequirementProfile analyze(TenderDocumentAnalysisInput input) {
        DocumentAnalysisInput genericInput = new DocumentAnalysisInput(
                String.valueOf(input.tenderId()),
                input.fileName(),
                input.extractedText(),
                input.structuredMetadata(),
                getStructuralChunker().chunk(input.extractedText(), input.structuredMetadata()),
                PROFILE_CODE,
                Map.of("projectId", input.projectId())
        );
        
        DocumentAnalysisResult result = this.analyze(genericInput);
        return mapToProfile(result);
    }

    @Override
    protected TenderRequirementOutput requestAi(String prompt) {
        OpenAiBidAgentRequestConfig config = configurationResolver.resolve(USE_CASE);
        return structuredOutputService.request(
                prompt,
                TenderRequirementOutput.class,
                config,
                "OpenAI structured response did not include tender requirements"
        );
    }

    @Override
    protected DocumentAnalysisResult mergeAndMap(DocumentAnalysisInput input, List<TenderRequirementOutput> results) {
        List<TenderRequirementProfile> profiles = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            profiles.add(toTenderProfile(results.get(i), input.chunks().get(i).sectionPath()));
        }
        TenderRequirementProfile merged = TenderRequirementProfileMerger.merge(profiles);
        
        return new DocumentAnalysisResult(
                input.documentId(),
                convertToMap(merged),
                merged.items().stream().map(this::toAnalysisItem).toList(),
                input.fullText(),
                List.of()
        );
    }

    private DocumentAnalysisResult.AnalysisRequirementItem toAnalysisItem(TenderRequirementItemSnapshot item) {
        return new DocumentAnalysisResult.AnalysisRequirementItem(
                item.category(), item.title(), item.content(), item.mandatory(),
                item.sourceExcerpt(), item.confidence(), item.sectionPath()
        );
    }

    private Map<String, Object> convertToMap(TenderRequirementProfile profile) {
        Map<String, Object> data = new HashMap<>();
        data.put("projectName", profile.projectName());
        data.put("purchaserName", profile.purchaserName());
        data.put("budget", profile.budget());
        data.put("publishDate", profile.publishDate());
        data.put("deadline", profile.deadline());
        return data;
    }

    private TenderRequirementProfile mapToProfile(DocumentAnalysisResult result) {
        Map<String, Object> data = result.extractedData();
        return new TenderRequirementProfile(
                (String) data.get("projectName"),
                null, null,
                (String) data.get("purchaserName"),
                (BigDecimal) data.get("budget"),
                null, null,
                (LocalDate) data.get("publishDate"),
                (LocalDateTime) data.get("deadline"),
                List.of(), List.of(), List.of(), List.of(), null, List.of(), List.of(), List.of(),
                result.requirements().stream().map(this::toSnapshot).toList()
        );
    }

    private TenderRequirementItemSnapshot toSnapshot(DocumentAnalysisResult.AnalysisRequirementItem item) {
        return new TenderRequirementItemSnapshot(
                item.category(), item.title(), item.content(), item.mandatory(), 
                item.sourceExcerpt(), item.confidence(), item.sectionPath()
        );
    }

    static String sanitizeUntrusted(String raw) {
        if (raw == null) return "";
        return raw.replace("<document>", "&lt;document&gt;").replace("</document>", "&lt;/document&gt;");
    }

    @Override
    protected String buildPrompt(DocumentAnalysisInput input, DocumentChunk chunk, int index, int total) {
        String safeChunk = sanitizeUntrusted(chunk.text());
        String safeFileName = sanitizeUntrusted(input.fileName());
        String sectionInfo = getSectionContext(chunk);
        
        return """
                你是招标文件解析 Agent。请读取招标文件正文，提取投标写作所需的结构化信息。
                %s
                项目ID: %s
                标讯ID: %s
                文件名: %s
                招标文件正文:
                <document>
                %s
                </document>
                """.formatted(
                sectionInfo,
                input.context().get("projectId"),
                input.documentId(),
                safeFileName,
                safeChunk
        );
    }

    private TenderRequirementProfile toTenderProfile(TenderRequirementOutput output, List<String> defaultPath) {
        String defaultPathStr = String.join(" > ", defaultPath);
        return new TenderRequirementProfile(
                output.projectName, output.tenderTitle, output.tenderScope, output.purchaserName,
                parseBudget(output.budget), output.region, output.industry,
                parsePublishDate(output.publishDate), parseDeadline(output.deadline),
                nullToList(output.qualificationRequirements), nullToList(output.technicalRequirements),
                nullToList(output.commercialRequirements), nullToList(output.scoringCriteria),
                output.deadlineText, nullToList(output.requiredMaterials), nullToList(output.riskPoints),
                nullToList(output.tags),
                nullToList(output.requirementItems).stream().map(item -> toTenderItem(item, defaultPathStr)).toList()
        );
    }

    private TenderRequirementItemSnapshot toTenderItem(TenderRequirementItemOutput item, String defaultPath) {
        String finalPath = item.sectionPath;
        if ((finalPath == null || finalPath.isBlank()) && !defaultPath.isEmpty()) {
            finalPath = defaultPath;
        }
        return new TenderRequirementItemSnapshot(
                item.category, item.title, item.content, item.mandatory,
                item.sourceExcerpt, item.confidence, finalPath
        );
    }

    static BigDecimal parseBudget(String value) {
        String normalized = normalizeNumber(value);
        if (normalized == null) return null;
        Matcher matcher = BUDGET_NUMBER.matcher(normalized);
        if (!matcher.matches()) return null;
        BigDecimal amount = new BigDecimal(matcher.group(1));
        String unit = matcher.group(2);
        return unit != null && unit.contains("万") ? amount.multiply(WAN_UNIT) : amount;
    }

    static LocalDate parsePublishDate(String value) {
        Matcher matcher = DATE_VALUE.matcher(trimToEmpty(value));
        return matcher.matches() ? dateFromParts(matcher.group(1), matcher.group(2), matcher.group(3)) : null;
    }

    static LocalDateTime parseDeadline(String value) {
        String trimmed = trimToEmpty(value);
        Matcher matcher = DATE_TIME_VALUE.matcher(trimmed);
        if (!matcher.matches()) {
            LocalDate date = parsePublishDate(trimmed);
            return date == null ? null : date.atTime(23, 59, 59);
        }
        return dateFromParts(matcher.group(1), matcher.group(2), matcher.group(3)).atTime(
                twoDigitInt(matcher.group(4)), twoDigitInt(matcher.group(5)), twoDigitInt(matcher.group(6))
        );
    }

    private static LocalDate dateFromParts(String yearText, String monthText, String dayText) {
        try {
            int y = Integer.parseInt(yearText);
            int m = Integer.parseInt(monthText);
            int d = Integer.parseInt(dayText);
            return LocalDate.of(y, m, d);
        } catch (NumberFormatException | java.time.DateTimeException e) { return null; }
    }

    private static Integer twoDigitInt(String value) {
        try { return Integer.parseInt(value); } catch (NumberFormatException e) { return null; }
    }

    private static String normalizeNumber(String value) {
        String trimmed = trimToEmpty(value);
        if (trimmed.isBlank() || trimmed.contains("约") || trimmed.contains("预计")) return null;
        return trimmed.replace(",", "").replace("人民币", "").trim();
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private <T> List<T> nullToList(List<T> values) {
        return values == null ? List.of() : values;
    }

    public static class TenderRequirementOutput {
        public String projectName;
        public String tenderTitle;
        public String tenderScope;
        public String purchaserName;
        public String budget;
        public String region;
        public String industry;
        public String publishDate;
        public String deadline;
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
        public String sectionPath;
    }
}
