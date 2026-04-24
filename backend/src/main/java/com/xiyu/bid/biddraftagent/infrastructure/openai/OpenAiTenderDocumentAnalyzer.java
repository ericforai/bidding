package com.xiyu.bid.biddraftagent.infrastructure.openai;

import com.xiyu.bid.biddraftagent.application.TenderDocumentAnalysisInput;
import com.xiyu.bid.biddraftagent.application.TenderDocumentAnalyzer;
import com.xiyu.bid.biddraftagent.domain.TenderRequirementItemSnapshot;
import com.xiyu.bid.biddraftagent.domain.TenderRequirementProfile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OpenAiTenderDocumentAnalyzer implements TenderDocumentAnalyzer {

    private static final int MAX_CHUNK_CHARS = 24000;
    private static final int CHUNK_OVERLAP_CHARS = 1200;
    private static final String USE_CASE = "tender document analysis";
    private static final BigDecimal WAN_UNIT = new BigDecimal("10000");
    private static final Pattern BUDGET_NUMBER = Pattern.compile("^(\\d+(?:\\.\\d{1,2})?)(?:\\s*(万元|万|元))?$");
    private static final Pattern DATE_VALUE = Pattern.compile("^(\\d{4})-(\\d{2})-(\\d{2})$");
    private static final Pattern DATE_TIME_VALUE = Pattern.compile("^(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2}):(\\d{2}):(\\d{2})$");

    private final OpenAiBidAgentConfigurationResolver configurationResolver;
    private final OpenAiStructuredOutputService structuredOutputService;

    public OpenAiTenderDocumentAnalyzer(
            OpenAiBidAgentConfigurationResolver configurationResolver,
            OpenAiStructuredOutputService structuredOutputService
    ) {
        this.configurationResolver = configurationResolver;
        this.structuredOutputService = structuredOutputService;
    }

    @Override
    public TenderRequirementProfile analyze(TenderDocumentAnalysisInput input) {
        List<String> chunks = TenderDocumentTextChunker.split(
                input.extractedText(),
                MAX_CHUNK_CHARS,
                CHUNK_OVERLAP_CHARS
        );
        List<TenderRequirementProfile> profiles = new ArrayList<>();
        for (int index = 0; index < chunks.size(); index++) {
            TenderRequirementOutput output = requestAnalysis(buildPrompt(input, chunks.get(index), index + 1, chunks.size()));
            profiles.add(toProfile(output));
        }
        return TenderRequirementProfileMerger.merge(profiles);
    }

    private TenderRequirementOutput requestAnalysis(String prompt) {
        OpenAiBidAgentRequestConfig config = configurationResolver.resolve(USE_CASE);
        return structuredOutputService.request(
                prompt,
                TenderRequirementOutput.class,
                config,
                "OpenAI structured response did not include tender requirements"
        );
    }

    private String buildPrompt(TenderDocumentAnalysisInput input, String chunkText, int chunkIndex, int chunkTotal) {
        return """
                你是招标文件解析 Agent。请读取招标文件正文，提取投标写作所需的结构化信息。
                必须遵守：
                - 当前正文是完整招标文件的第 %s/%s 片，请只从本片正文中提取，无法确认的字段留空，不要编造。
                - requirementItems 必须逐条列出关键要求，至少覆盖资格、技术、商务、评分和材料清单中出现的要求。
                - category 只能使用 qualification、technical、commercial、pricing、legal、delivery、scoring、material、other。
                - mandatory 表示是否为必须响应/必须提供。
                - sourceExcerpt 保留能定位来源的短句，confidence 使用 0-100 整数。
                - budget 表示项目预算，必须统一为人民币元数字字符串，例如 6800000 或 6800000.50；无法确认留空，不得根据“约”“预计”等表述推断。
                - region 表示项目所属地区；industry 表示行业分类；无法从正文确认则留空，不得推断。
                - publishDate 使用 yyyy-MM-dd；deadline 使用 yyyy-MM-dd'T'HH:mm:ss；如果正文只有截止日期没有时间，可输出 yyyy-MM-dd，系统会按 23:59:59 补齐；deadlineText 可保留原文截止时间描述。
                - 所有字段只能来自本片正文，无法确认的字段留空，不得推断。
                - 返回结构化字段 projectName、tenderTitle、tenderScope、purchaserName、budget、region、industry、publishDate、deadline、qualificationRequirements、technicalRequirements、commercialRequirements、scoringCriteria、deadlineText、requiredMaterials、riskPoints、tags、requirementItems。

                项目ID: %s
                标讯ID: %s
                文件名: %s
                招标文件正文:
                %s
                """.formatted(
                chunkIndex,
                chunkTotal,
                input.projectId(),
                input.tenderId(),
                input.fileName(),
                chunkText
        );
    }

    private TenderRequirementProfile toProfile(TenderRequirementOutput output) {
        return new TenderRequirementProfile(
                output.projectName,
                output.tenderTitle,
                output.tenderScope,
                output.purchaserName,
                parseBudget(output.budget),
                output.region,
                output.industry,
                parsePublishDate(output.publishDate),
                parseDeadline(output.deadline),
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

    static BigDecimal parseBudget(String value) {
        String normalized = normalizeNumber(value);
        if (normalized == null) {
            return null;
        }
        Matcher matcher = BUDGET_NUMBER.matcher(normalized);
        if (!matcher.matches()) {
            return null;
        }
        BigDecimal amount = new BigDecimal(matcher.group(1));
        String unit = matcher.group(2);
        return unit != null && unit.contains("万") ? amount.multiply(WAN_UNIT) : amount;
    }

    static LocalDate parsePublishDate(String value) {
        Matcher matcher = DATE_VALUE.matcher(trimToEmpty(value));
        if (!matcher.matches()) {
            return null;
        }
        return dateFromParts(matcher.group(1), matcher.group(2), matcher.group(3));
    }

    static LocalDateTime parseDeadline(String value) {
        String trimmed = trimToEmpty(value);
        Matcher matcher = DATE_TIME_VALUE.matcher(trimmed);
        if (!matcher.matches()) {
            LocalDate date = parsePublishDate(trimmed);
            return date == null ? null : date.atTime(23, 59, 59);
        }
        LocalDate date = dateFromParts(matcher.group(1), matcher.group(2), matcher.group(3));
        Integer hour = twoDigitInt(matcher.group(4));
        Integer minute = twoDigitInt(matcher.group(5));
        Integer second = twoDigitInt(matcher.group(6));
        if (date == null || hour == null || hour > 23 || minute == null || minute > 59 || second == null || second > 59) {
            return null;
        }
        return date.atTime(hour, minute, second);
    }

    private static LocalDate dateFromParts(String yearText, String monthText, String dayText) {
        Integer year = fourDigitInt(yearText);
        Integer month = twoDigitInt(monthText);
        Integer day = twoDigitInt(dayText);
        if (year == null || month == null || month < 1 || month > 12 || day == null || day < 1) {
            return null;
        }
        int maxDay = YearMonth.of(year, month).lengthOfMonth();
        if (day > maxDay) {
            return null;
        }
        return LocalDate.of(year, month, day);
    }

    private static Integer fourDigitInt(String value) {
        if (value == null || value.length() != 4) {
            return null;
        }
        return Integer.parseInt(value);
    }

    private static Integer twoDigitInt(String value) {
        if (value == null || value.length() != 2) {
            return null;
        }
        return Integer.parseInt(value);
    }

    private static String normalizeNumber(String value) {
        String trimmed = trimToEmpty(value);
        if (trimmed.isBlank()) {
            return null;
        }
        if (trimmed.contains("约") || trimmed.contains("预计") || trimmed.contains("左右")) {
            return null;
        }
        return trimmed
                .replace(",", "")
                .replace("，", "")
                .replace("人民币", "")
                .trim();
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
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
    }
}
