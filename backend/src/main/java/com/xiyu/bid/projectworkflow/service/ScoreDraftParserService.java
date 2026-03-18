// Input: projectworkflow repositories, DTOs, and support services
// Output: Score Draft Parser business service operations
// Pos: Service/业务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.projectworkflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiyu.bid.projectworkflow.entity.ProjectScoreDraft;
import lombok.RequiredArgsConstructor;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ScoreDraftParserService {

    private static final Pattern SECTION_HEADING = Pattern.compile(
            "^(?:(?:第[一二三四五六七八九十]+章)?\\s*)?(?:\\d+(?:\\.\\d+)*)?\\s*.*?(商务|技术|价格)评分标准(?:表|如下|说明)?(?:（[^）]+）)?$"
    );
    private static final Pattern DIGITS_ONLY = Pattern.compile("^\\d+(?:\\.\\d+)?$");
    private static final Pattern SCORE_IN_RULE = Pattern.compile("(最高\\s*\\d+(?:\\.\\d+)?分|满分\\s*\\d+(?:\\.\\d+)?分|得\\s*\\d+(?:\\.\\d+)?分|\\d+(?:\\.\\d+)?分)");
    private static final Set<String> TABLE_HEADERS = Set.of(
            "序号", "评价项目", "评分标准", "评标分值", "评审内容", "评分因素", "分值", "评分细则",
            "评分标准说明", "评分项目", "分数", "评分因素及标准", "条款号", "详细", "评审", "标准"
    );
    private static final List<String> RULE_STARTERS = List.of(
            "供应商", "根据", "按", "提供", "最大程度", "接受平台", "结算周期",
            "办公用品", "方案应包含", "1、", "（一）", "若该商品"
    );
    private static final List<String> TITLE_REJECT_PREFIXES = List.of(
            "根据", "提供", "最大程度", "接受平台", "办公用品", "方案应包含", "1、", "（一）", "若该商品"
    );

    private final ObjectMapper objectMapper;

    public List<ProjectScoreDraft> parse(Long projectId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请上传评分标准文件");
        }

        String fileName = Optional.ofNullable(file.getOriginalFilename())
                .filter(name -> !name.isBlank())
                .orElse("评分标准文件");
        FileType fileType = detectFileType(fileName);
        String extractedText = extractText(file, fileType);
        List<ProjectScoreDraft> drafts = parseExtractedText(projectId, fileName, extractedText);
        if (drafts.isEmpty()) {
            throw new IllegalArgumentException("未识别到规整评分标准，请优先上传包含商务/技术/价格评分表的 Word 文件");
        }
        return drafts;
    }

    List<ProjectScoreDraft> parseExtractedText(Long projectId, String fileName, String extractedText) {
        List<String> lines = Arrays.stream(normalizeExtractedText(extractedText).split("\n"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();

        Map<String, List<String>> sections = splitSections(lines);
        List<ProjectScoreDraft> drafts = new ArrayList<>();
        int sectionIndex = 0;

        for (Map.Entry<String, List<String>> entry : sections.entrySet()) {
            List<DraftSeed> seeds = switch (entry.getKey()) {
                case "business" -> parseBusinessSection(entry.getValue());
                case "technical" -> parseTechnicalSection(entry.getValue());
                case "price" -> parsePriceSection(entry.getValue());
                default -> List.of();
            };

            int rowIndex = 0;
            for (DraftSeed seed : seeds) {
                drafts.add(buildDraft(projectId, fileName, entry.getKey(), seed, sectionIndex, rowIndex++));
            }
            sectionIndex++;
        }

        return drafts;
    }

    private FileType detectFileType(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".docx")) {
            return FileType.DOCX;
        }
        if (lower.endsWith(".doc")) {
            return FileType.DOC;
        }
        if (lower.endsWith(".pdf")) {
            throw new IllegalArgumentException("暂不支持 PDF 评分表解析，请优先上传 Word 版本评分标准");
        }
        throw new IllegalArgumentException("仅支持 .doc/.docx 评分文件");
    }

    private String extractText(MultipartFile file, FileType fileType) {
        try (InputStream inputStream = file.getInputStream()) {
            return switch (fileType) {
                case DOCX -> extractDocxText(inputStream);
                case DOC -> extractDocText(inputStream);
            };
        } catch (IOException ex) {
            throw new IllegalStateException("读取评分标准文件失败", ex);
        }
    }

    private String extractDocxText(InputStream inputStream) throws IOException {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            StringBuilder text = new StringBuilder();
            for (IBodyElement element : document.getBodyElements()) {
                switch (element.getElementType()) {
                    case PARAGRAPH -> appendParagraph(text, (XWPFParagraph) element);
                    case TABLE -> appendTable(text, (XWPFTable) element);
                    default -> {
                    }
                }
            }
            if (text.length() == 0) {
                try (XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                    text.append(extractor.getText());
                }
            }
            return text.toString();
        }
    }

    private String extractDocText(InputStream inputStream) throws IOException {
        try (HWPFDocument document = new HWPFDocument(inputStream);
             WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText();
        }
    }

    private void appendParagraph(StringBuilder text, XWPFParagraph paragraph) {
        String line = normalizeInlineText(paragraph.getText());
        if (!line.isBlank()) {
            text.append(line).append('\n');
        }
    }

    private void appendTable(StringBuilder text, XWPFTable table) {
        for (XWPFTableRow row : table.getRows()) {
            List<String> cells = row.getTableCells().stream()
                    .map(XWPFTableCell::getText)
                    .map(this::normalizeInlineText)
                    .filter(cell -> !cell.isBlank())
                    .toList();
            if (!cells.isEmpty()) {
                text.append(String.join("\u0007", cells)).append('\n');
            }
        }
    }

    private String normalizeInlineText(String text) {
        return Optional.ofNullable(text)
                .orElse("")
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String normalizeExtractedText(String text) {
        return Optional.ofNullable(text)
                .orElse("")
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace('\u0007', '\n')
                .replace('\u000b', '\n')
                .replace('\u000c', '\n')
                .replace('\u00A0', ' ')
                .replaceAll("[\\t]+", "\n")
                .replaceAll(" +", " ")
                .replaceAll("\\n{2,}", "\n");
    }

    private Map<String, List<String>> splitSections(List<String> lines) {
        Map<String, Integer> starts = new LinkedHashMap<>();
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            if (line.contains("详见第三章") || line.contains("详见评审程序及办法")) {
                continue;
            }
            Matcher matcher = SECTION_HEADING.matcher(line);
            if (matcher.matches()) {
                String category = switch (matcher.group(1)) {
                    case "商务" -> "business";
                    case "技术" -> "technical";
                    case "价格" -> "price";
                    default -> null;
                };
                if (category != null && !starts.containsKey(category)) {
                    starts.put(category, index);
                }
            }
        }

        Map<String, List<String>> sections = new LinkedHashMap<>();
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(starts.entrySet());
        for (int index = 0; index < entries.size(); index++) {
            int start = entries.get(index).getValue() + 1;
            int end = index + 1 < entries.size() ? entries.get(index + 1).getValue() : lines.size();
            sections.put(entries.get(index).getKey(), lines.subList(start, end));
        }
        return sections;
    }

    private List<DraftSeed> parseBusinessSection(List<String> sectionLines) {
        return parseSerialChunks(sectionLines, false);
    }

    private List<DraftSeed> parsePriceSection(List<String> sectionLines) {
        return parseSerialChunks(sectionLines, true);
    }

    private List<DraftSeed> parseSerialChunks(List<String> sectionLines, boolean scoreBeforeRule) {
        List<String> lines = sectionLines.stream()
                .map(this::normalizeLineForParsing)
                .filter(line -> !line.isBlank())
                .filter(line -> !TABLE_HEADERS.contains(line))
                .takeWhile(line -> !line.startsWith("3.9 ") && !line.equals("合计"))
                .toList();

        List<List<String>> chunks = new ArrayList<>();
        List<String> current = null;
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            if (looksLikeRowStart(lines, index)) {
                if (current != null && !current.isEmpty()) {
                    chunks.add(current);
                }
                current = new ArrayList<>();
            }
            if (current != null) {
                current.add(line);
            }
        }
        if (current != null && !current.isEmpty()) {
            chunks.add(current);
        }

        List<DraftSeed> drafts = new ArrayList<>();
        for (List<String> chunk : chunks) {
            drafts.addAll(parseSerialChunk(chunk, scoreBeforeRule));
        }
        return drafts;
    }

    private boolean looksLikeRowStart(List<String> lines, int index) {
        if (!isSerialLine(lines.get(index)) || index + 1 >= lines.size()) {
            return false;
        }
        return isPotentialTitle(lines.get(index + 1));
    }

    private boolean isPotentialTitle(String line) {
        if (line == null) {
            return false;
        }
        String text = line.trim();
        if (text.isBlank() || TABLE_HEADERS.contains(text) || isScoreOnlyLine(text)) {
            return false;
        }
        if (text.length() > 80 || text.contains("http")) {
            return false;
        }
        if (text.contains("，") || text.contains("。") || text.contains("；") || text.contains("：") || text.contains("得")) {
            return false;
        }
        return TITLE_REJECT_PREFIXES.stream().noneMatch(text::startsWith);
    }

    private List<DraftSeed> parseSerialChunk(List<String> chunk, boolean scoreBeforeRule) {
        if (chunk.size() < 2) {
            return List.of();
        }

        String title = cleanTitle(chunk.get(1));
        if (title.isBlank()) {
            return List.of();
        }

        List<String> remaining = new ArrayList<>(chunk.subList(2, chunk.size()));
        if (remaining.isEmpty()) {
            return List.of();
        }

        if (scoreBeforeRule && isScoreOnlyLine(remaining.get(0)) && remaining.size() > 1) {
            String score = formatScoreText(remaining.remove(0));
            return List.of(buildSeed(title, title, joinLines(remaining), score));
        }

        List<DraftSeed> results = new ArrayList<>();
        List<String> buffer = new ArrayList<>();
        int unitIndex = 1;
        for (String line : remaining) {
            if (isScoreOnlyLine(line) && !buffer.isEmpty()) {
                String unitTitle = unitIndex == 1 ? title : title + "（子项" + unitIndex + "）";
                results.add(buildSeed(unitTitle, title, joinLines(buffer), formatScoreText(line)));
                buffer.clear();
                unitIndex++;
            } else {
                buffer.add(line);
            }
        }

        if (!buffer.isEmpty()) {
            String merged = joinLines(buffer);
            String inferredScore = inferScoreText(merged);
            results.add(buildSeed(unitIndex == 1 ? title : title + "（子项" + unitIndex + "）", title, merged, inferredScore));
        }
        return results;
    }

    private List<DraftSeed> parseTechnicalSection(List<String> sectionLines) {
        List<String> lines = sectionLines.stream()
                .map(this::normalizeLineForParsing)
                .filter(line -> !line.isBlank())
                .filter(line -> !TABLE_HEADERS.contains(line))
                .takeWhile(line -> !line.equals("合计") && !line.startsWith("3.8 "))
                .toList();

        List<DraftSeed> drafts = new ArrayList<>();
        int index = 0;
        while (index < lines.size()) {
            String title = cleanTitle(lines.get(index));
            if (title.isBlank()) {
                index++;
                continue;
            }
            if (index + 1 >= lines.size() || !isScoreOnlyLine(lines.get(index + 1))) {
                index++;
                continue;
            }

            String score = formatScoreText(lines.get(index + 1));
            index += 2;
            List<String> ruleLines = new ArrayList<>();
            while (index < lines.size()) {
                if (index + 1 < lines.size() && !isScoreOnlyLine(lines.get(index)) && isScoreOnlyLine(lines.get(index + 1))) {
                    break;
                }
                if (lines.get(index).equals("合计")) {
                    break;
                }
                ruleLines.add(lines.get(index));
                index++;
            }

            String ruleText = joinLines(ruleLines);
            if (!ruleText.isBlank()) {
                drafts.addAll(buildTechnicalSeeds(title, ruleText, score));
            }
        }
        return drafts;
    }

    private List<DraftSeed> buildTechnicalSeeds(String title, String ruleText, String score) {
        List<String> clauses = splitNumberedClauses(ruleText);
        List<String> scoredClauses = clauses.stream()
                .map(String::trim)
                .filter(clause -> !clause.isBlank())
                .filter(clause -> inferScoreText(clause) != null)
                .toList();

        if (scoredClauses.size() <= 1) {
            return List.of(buildSeed(title, title, ruleText, score));
        }

        List<DraftSeed> seeds = new ArrayList<>();
        for (int index = 0; index < scoredClauses.size(); index++) {
            String clauseTitle = title + "（子项" + (index + 1) + "）";
            seeds.add(buildSeed(clauseTitle, title, scoredClauses.get(index), inferScoreText(scoredClauses.get(index))));
        }
        return seeds;
    }

    private List<String> splitNumberedClauses(String text) {
        String normalized = Optional.ofNullable(text).orElse("")
                .replaceAll("(?<!^)\\s*(?=(?:\\d+、|（[一二三四五六七八九十]+）))", "\n");
        return Arrays.stream(normalized.split("\n"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
    }

    private DraftSeed buildSeed(String scoreItemTitle, String baseTitle, String ruleText, String scoreText) {
        String normalizedRule = normalizeRuleText(ruleText);
        String normalizedScore = normalizeScoreText(scoreText, normalizedRule);
        String taskAction = inferTaskAction(baseTitle, normalizedRule);
        return new DraftSeed(
                scoreItemTitle,
                normalizedRule,
                normalizedScore,
                taskAction,
                buildTaskTitle(taskAction, scoreItemTitle, normalizedScore),
                buildDescription(scoreItemTitle, normalizedScore, normalizedRule),
                inferDeliverables(baseTitle, normalizedRule)
        );
    }

    private String normalizeLineForParsing(String line) {
        return Optional.ofNullable(line)
                .orElse("")
                .replaceAll("^第[一二三四五六七八九十]+章.*$", "")
                .replaceAll("^条款号$", "")
                .replaceAll("^详细$", "")
                .replaceAll("^评审$", "")
                .replaceAll("^标准$", "")
                .trim();
    }

    private boolean isSerialLine(String line) {
        return DIGITS_ONLY.matcher(line).matches();
    }

    private boolean isScoreOnlyLine(String line) {
        return DIGITS_ONLY.matcher(line).matches() || line.matches("^(?:最高|满分)?\\s*\\d+(?:\\.\\d+)?分$");
    }

    private String cleanTitle(String raw) {
        String title = Optional.ofNullable(raw).orElse("").trim();
        if (title.isBlank()) {
            return "";
        }

        Matcher combined = Pattern.compile("^(\\d+)\\s*(.+)$").matcher(title);
        if (combined.matches()) {
            title = combined.group(2).trim();
        }

        for (String starter : RULE_STARTERS) {
            int index = title.indexOf(starter);
            if (index > 0) {
                return title.substring(0, index).trim();
            }
        }
        return title;
    }

    private String joinLines(List<String> lines) {
        return lines.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
    }

    private String normalizeRuleText(String ruleText) {
        return Optional.ofNullable(ruleText)
                .orElse("")
                .replaceAll("\\n{2,}", "\n")
                .trim();
    }

    private String normalizeScoreText(String scoreText, String ruleText) {
        if (scoreText != null && !scoreText.isBlank()) {
            return formatScoreText(scoreText);
        }
        String inferred = inferScoreText(ruleText);
        return inferred != null ? inferred : "未明确分值";
    }

    private String inferScoreText(String ruleText) {
        if (ruleText == null || ruleText.isBlank()) {
            return null;
        }
        Matcher matcher = SCORE_IN_RULE.matcher(ruleText);
        String preferred = null;
        BigDecimal maxValue = BigDecimal.valueOf(-1);
        String maxScoreText = null;
        while (matcher.find()) {
            String candidate = matcher.group(1).replace("得", "").trim();
            if (candidate.contains("最高") || candidate.contains("满分")) {
                preferred = candidate;
            }
            BigDecimal numericValue = extractNumericScore(candidate);
            if (numericValue.compareTo(maxValue) > 0) {
                maxValue = numericValue;
                maxScoreText = candidate;
            }
        }
        if (preferred != null) {
            return formatScoreText(preferred);
        }
        return maxScoreText != null ? formatScoreText(maxScoreText) : null;
    }

    private BigDecimal extractNumericScore(String scoreText) {
        Matcher matcher = Pattern.compile("(\\d+(?:\\.\\d+)?)").matcher(Optional.ofNullable(scoreText).orElse(""));
        if (matcher.find()) {
            return new BigDecimal(matcher.group(1));
        }
        return BigDecimal.ZERO;
    }

    private String formatScoreText(String raw) {
        String text = Optional.ofNullable(raw).orElse("").trim();
        if (text.isBlank()) {
            return "未明确分值";
        }
        if (text.contains("分")) {
            return text.replaceAll("\\s+", "");
        }
        return text + "分";
    }

    private String inferTaskAction(String title, String ruleText) {
        String text = (Optional.ofNullable(title).orElse("") + " " + Optional.ofNullable(ruleText).orElse("")).toLowerCase(Locale.ROOT);
        if (containsAny(text, "资质", "证书", "认证", "许可")) {
            return "准备";
        }
        if (containsAny(text, "业绩", "案例", "合同", "财务报表")) {
            return "整理";
        }
        if (containsAny(text, "报价", "折扣率", "价格", "结算周期")) {
            return "复核";
        }
        if (containsAny(text, "方案", "实施", "服务", "仓储", "配送", "对接")) {
            return "编写";
        }
        return "处理";
    }

    private boolean containsAny(String text, String... keywords) {
        return Arrays.stream(keywords).anyMatch(text::contains);
    }

    private String buildTaskTitle(String action, String title, String scoreText) {
        return action + title + "（" + scoreText + "）";
    }

    private String buildDescription(String scoreItemTitle,
                                    String scoreValueText,
                                    String scoreRuleText) {
        return """
                评分目标：%s
                分值规则：%s
                评分原文：%s
                执行要求：请准备该项得分所需材料，并确保响应内容可以直接支撑评审打分。
                完成标准：材料齐全、论据清晰、可直接支撑该项得分判断。
                """.formatted(scoreItemTitle, scoreValueText, scoreRuleText);
    }

    private List<String> inferDeliverables(String title, String ruleText) {
        String text = Optional.ofNullable(title).orElse("") + "\n" + Optional.ofNullable(ruleText).orElse("");
        Set<String> deliverables = new LinkedHashSet<>();

        if (containsAny(text, "资质", "证书", "认证", "许可证")) {
            deliverables.add("资质证书复印件");
            deliverables.add("有效期说明");
        }
        if (containsAny(text, "业绩", "案例", "合同")) {
            deliverables.add("合同关键页");
            deliverables.add("验收证明");
            deliverables.add("项目简介");
        }
        if (containsAny(text, "财务", "营业收入", "审计")) {
            deliverables.add("审计报告或财务报表");
        }
        if (containsAny(text, "方案", "实施", "仓储", "配送", "对接", "服务")) {
            deliverables.add("方案正文");
            deliverables.add("实施或服务说明");
        }
        if (containsAny(text, "价格", "报价", "折扣率", "结算周期")) {
            deliverables.add("报价表");
            deliverables.add("测算依据");
            deliverables.add("承诺函或说明");
        }
        if (deliverables.isEmpty()) {
            deliverables.add("响应说明材料");
        }
        return new ArrayList<>(deliverables);
    }

    private ProjectScoreDraft buildDraft(Long projectId,
                                         String fileName,
                                         String category,
                                         DraftSeed seed,
                                         int tableIndex,
                                         int rowIndex) {
        return ProjectScoreDraft.builder()
                .projectId(projectId)
                .sourceFileName(fileName)
                .category(category)
                .scoreItemTitle(seed.scoreItemTitle())
                .scoreRuleText(seed.scoreRuleText())
                .scoreValueText(seed.scoreValueText())
                .taskAction(seed.taskAction())
                .generatedTaskTitle(seed.generatedTaskTitle())
                .generatedTaskDescription(seed.generatedTaskDescription())
                .suggestedDeliverables(serializeDeliverables(seed.deliverables()))
                .status(ProjectScoreDraft.Status.DRAFT)
                .sourcePage(null)
                .sourceTableIndex(tableIndex)
                .sourceRowIndex(rowIndex)
                .build();
    }

    private String serializeDeliverables(List<String> deliverables) {
        try {
            return objectMapper.writeValueAsString(deliverables);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("评分草稿交付物序列化失败", ex);
        }
    }

    private record DraftSeed(
            String scoreItemTitle,
            String scoreRuleText,
            String scoreValueText,
            String taskAction,
            String generatedTaskTitle,
            String generatedTaskDescription,
            List<String> deliverables
    ) {
    }

    private enum FileType {
        DOC,
        DOCX
    }
}
