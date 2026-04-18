package com.xiyu.bid.projectworkflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiyu.bid.projectworkflow.parser.ScoreDraftDraftAssembler;
import com.xiyu.bid.projectworkflow.parser.ScoreDraftFileTypePolicy;
import com.xiyu.bid.projectworkflow.parser.ScoreDraftTextParser;
import com.xiyu.bid.projectworkflow.parser.WordTextExtractor;
import com.xiyu.bid.projectworkflow.entity.ProjectScoreDraft;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScoreDraftParserServiceTest {

    private final ScoreDraftParserService parserService = new ScoreDraftParserService(
            new ScoreDraftFileTypePolicy(),
            new WordTextExtractor(),
            new ScoreDraftTextParser(),
            new ScoreDraftDraftAssembler(new ObjectMapper())
    );

    @Test
    void parse_ShouldRejectEmptyInput() {
        MockMultipartFile file = new MockMultipartFile("file", "评分标准.docx", "application/octet-stream", new byte[0]);

        assertThatThrownBy(() -> parserService.parse(1001L, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("请上传评分标准文件");
    }

    @Test
    void parse_ShouldReadDocxAndExtractScoreDrafts() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "评分标准.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                buildTableDocx()
        );

        List<ProjectScoreDraft> drafts = parserService.parse(1002L, file);

        assertThat(drafts).hasSize(3);
        assertThat(drafts).extracting(ProjectScoreDraft::getCategory)
                .containsExactly("business", "technical", "price");
    }

    @Test
    void parse_ShouldRejectPdfInput() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "评分标准.pdf",
                "application/pdf",
                "fake".getBytes(StandardCharsets.UTF_8)
        );

        assertThatThrownBy(() -> parserService.parse(1003L, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("暂不支持 PDF");
    }

    @Test
    void parse_ShouldRejectUnrecognizedScoreDrafts() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "评分标准.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                buildDocx("普通说明文字\n没有评分章节")
        );

        assertThatThrownBy(() -> parserService.parse(1004L, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("未识别到规整评分标准");
    }

    private byte[] buildDocx(String content) throws Exception {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            for (String line : content.split("\n")) {
                document.createParagraph().createRun().setText(line);
            }
            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] buildTableDocx() throws Exception {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText("第三章 评审程序及办法");
            document.createParagraph().createRun().setText("3.6 商务评分标准（20分）");

            XWPFTable businessTable = document.createTable(2, 4);
            setRow(businessTable, 0, "序号", "评价项目", "评分标准", "评标分值");
            setRow(businessTable, 1, "1", "同类项目业绩", "每提供1个同类项目业绩得2分，最高6分。", "6");

            document.createParagraph().createRun().setText("3.7 技术评分标准（50分）");
            XWPFTable technicalTable = document.createTable(2, 3);
            setRow(technicalTable, 0, "评分项目", "分数", "评分因素及标准");
            setRow(technicalTable, 1, "整体方案", "15", "最大程度满足采购文件要求。");

            document.createParagraph().createRun().setText("3.8 价格评分标准（30分）");
            XWPFTable priceTable = document.createTable(2, 4);
            setRow(priceTable, 0, "序号", "评价项目", "分值", "评分细则");
            setRow(priceTable, 1, "1", "品类折扣率", "25", "接受平台的价格管控，确保用户单位享受优惠价格。");

            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void setRow(XWPFTable table, int rowIndex, String... values) {
        for (int cellIndex = 0; cellIndex < values.length; cellIndex++) {
            table.getRow(rowIndex).getCell(cellIndex).setText(values[cellIndex]);
        }
    }
}
