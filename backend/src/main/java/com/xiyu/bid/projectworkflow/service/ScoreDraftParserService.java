package com.xiyu.bid.projectworkflow.service;

import com.xiyu.bid.projectworkflow.entity.ProjectScoreDraft;
import lombok.RequiredArgsConstructor;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.hwpf.usermodel.Table;
import org.apache.poi.hwpf.usermodel.TableIterator;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ScoreDraftParserService {

    private static final Set<String> SKIP_KEYWORDS = Set.of("总分", "合计", "备注", "说明");

    private final ScoreDraftGenerationService generationService;

    public List<ProjectScoreDraft> parse(Long projectId, MultipartFile file) throws IOException {
        String originalFilename = Optional.ofNullable(file.getOriginalFilename()).orElse("评分文件");
        String lowerName = originalFilename.toLowerCase(Locale.ROOT);
        if (lowerName.endsWith(".docx")) {
            try (InputStream inputStream = file.getInputStream();
                 XWPFDocument document = new XWPFDocument(inputStream)) {
                return parseDocx(projectId, originalFilename, document.getTables());
            }
        }
        if (lowerName.endsWith(".doc")) {
            try (InputStream inputStream = file.getInputStream();
                 HWPFDocument document = new HWPFDocument(inputStream)) {
                return parseDoc(projectId, originalFilename, document);
            }
        }
        throw new IllegalArgumentException("仅支持 .doc 和 .docx 评分文件");
    }

    private List<ProjectScoreDraft> parseDocx(Long projectId, String sourceFileName, List<XWPFTable> tables) {
        List<ProjectScoreDraft> drafts = new ArrayList<>();
        for (int tableIndex = 0; tableIndex < tables.size(); tableIndex++) {
            XWPFTable table = tables.get(tableIndex);
            List<List<String>> rows = new ArrayList<>();
            for (XWPFTableRow row : table.getRows()) {
                List<String> cells = new ArrayList<>();
                for (XWPFTableCell cell : row.getTableCells()) {
                    cells.add(cleanCellText(cell.getText()));
                }
                rows.add(cells);
            }
            drafts.addAll(parseTableRows(projectId, sourceFileName, tableIndex, rows));
        }
        return drafts;
    }

    private List<ProjectScoreDraft> parseDoc(Long projectId, String sourceFileName, HWPFDocument document) {
        List<ProjectScoreDraft> drafts = new ArrayList<>();
        Range range = document.getRange();
        TableIterator iterator = new TableIterator(range);
        int tableIndex = 0;
        while (iterator.hasNext()) {
            Table table = iterator.next();
            List<List<String>> rows = new ArrayList<>();
            for (int rowIndex = 0; rowIndex < table.numRows(); rowIndex++) {
                var row = table.getRow(rowIndex);
                List<String> cells = new ArrayList<>();
                for (int cellIndex = 0; cellIndex < row.numCells(); cellIndex++) {
                    cells.add(cleanCellText(row.getCell(cellIndex).text()));
                }
                rows.add(cells);
            }
            drafts.addAll(parseTableRows(projectId, sourceFileName, tableIndex, rows));
            tableIndex++;
        }
        return drafts;
    }

    private List<ProjectScoreDraft> parseTableRows(Long projectId,
                                                   String sourceFileName,
                                                   int tableIndex,
                                                   List<List<String>> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        List<String> headerRow = rows.get(0);
        HeaderMapping mapping = HeaderMapping.from(headerRow);
        if (!mapping.scoringTable()) {
            return List.of();
        }
        String category = generationService.detectCategory(String.join(" ", headerRow));
        List<ProjectScoreDraft> drafts = new ArrayList<>();
        for (int rowIndex = 1; rowIndex < rows.size(); rowIndex++) {
            List<String> row = rows.get(rowIndex);
            String title = mapping.cell(row, mapping.titleIndex);
            String ruleText = mapping.cell(row, mapping.ruleIndex);
            String scoreValueText = mapping.cell(row, mapping.scoreIndex);
            if (shouldSkipRow(title, ruleText, scoreValueText)) {
                continue;
            }
            String action = generationService.detectAction(title, ruleText);
            drafts.add(ProjectScoreDraft.builder()
                    .projectId(projectId)
                    .sourceFileName(sourceFileName)
                    .category(category)
                    .scoreItemTitle(title)
                    .scoreRuleText(ruleText)
                    .scoreValueText(scoreValueText)
                    .taskAction(action)
                    .generatedTaskTitle(generationService.generateTaskTitle(action, title, scoreValueText))
                    .generatedTaskDescription(generationService.generateTaskDescription(
                            sourceFileName, category, title, ruleText, scoreValueText, tableIndex, rowIndex))
                    .suggestedDeliverables(serializeDeliverables(
                            generationService.suggestDeliverables(title, ruleText, category)))
                    .status(ProjectScoreDraft.Status.DRAFT)
                    .sourcePage(null)
                    .sourceTableIndex(tableIndex)
                    .sourceRowIndex(rowIndex)
                    .build());
        }
        return drafts;
    }

    private boolean shouldSkipRow(String title, String ruleText, String scoreValueText) {
        String allText = (title + " " + ruleText + " " + scoreValueText).trim();
        if (allText.isBlank()) {
            return true;
        }
        return SKIP_KEYWORDS.stream().anyMatch(allText::contains);
    }

    private String serializeDeliverables(List<String> deliverables) {
        return deliverables.stream()
                .map(value -> "\"" + value.replace("\"", "\\\"") + "\"")
                .reduce("[", (left, right) -> "[".equals(left) ? left + right : left + "," + right)
                + "]";
    }

    private String cleanCellText(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\u0007', ' ')
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replace('\t', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private record HeaderMapping(int titleIndex, int ruleIndex, int scoreIndex) {

        private static final List<String> TITLE_HEADERS = Arrays.asList("评分项", "评审因素", "评价内容", "评审内容", "项目");
        private static final List<String> RULE_HEADERS = Arrays.asList("评分标准", "评分办法", "评审标准", "评审办法", "得分规则");
        private static final List<String> SCORE_HEADERS = Arrays.asList("分值", "满分", "得分");

        static HeaderMapping from(List<String> headerRow) {
            int titleIndex = findIndex(headerRow, TITLE_HEADERS);
            int ruleIndex = findIndex(headerRow, RULE_HEADERS);
            int scoreIndex = findIndex(headerRow, SCORE_HEADERS);
            return new HeaderMapping(titleIndex, ruleIndex, scoreIndex);
        }

        boolean scoringTable() {
            return titleIndex >= 0 && ruleIndex >= 0;
        }

        String cell(List<String> row, int index) {
            if (index < 0 || index >= row.size()) {
                return "";
            }
            return row.get(index);
        }

        private static int findIndex(List<String> values, List<String> candidates) {
            for (int i = 0; i < values.size(); i++) {
                String current = values.get(i);
                for (String candidate : candidates) {
                    if (current.contains(candidate)) {
                        return i;
                    }
                }
            }
            return -1;
        }
    }
}
