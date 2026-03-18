package com.xiyu.bid.projectworkflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    private final ScoreDraftParserService parserService = new ScoreDraftParserService(new ObjectMapper());

    @Test
    void parseExtractedText_ShouldSplitRegularScoreSectionsIntoMinimalUnits() {
        String text = """
                第三章 评审程序及办法
                3.6 商务评分标准（20分）
                序号
                评价项目
                评分标准
                评标分值
                1
                供应商综合实力
                注册资金10000万元及以上，得2分，其他得0分。
                2
                2022、2023连续2年，供应商年度营业收入累计不低于40亿元人民币，得2分，其他得0分。
                2
                2
                供应商资质
                供应商具备ISO14001环境管理体系证书、质量管理体系认证证书ISO9001、ISO45001职业健康安全管理体系证书；每个资质证书2分，满分6分。
                6
                3.7 技术评分标准（50分）
                评分项目
                分数
                评分因素及标准
                整体方案
                15
                最大程度满足采购文件要求，从科学性、先进性、可行性等综合评价，按照优、良、中、差，依次得14-15分、9-13分、3-8分、0-2分。
                平台系统对接实施方案
                3
                根据本项目采购文件要求做平台系统对接实施方案，从方案质量、合理性、适用性等进行综合评审。
                3.8 价格评分标准（30分）
                序号
                评价项目
                分值
                评分细则
                1
                品类折扣率
                25
                接受平台的价格管控，确保用户单位享受优惠价格。平均折扣率等于基准价的得25分。
                2
                结算周期
                5
                结算周期：采购单位收到发票后90个自然日现汇得2分，采购单位收到发票付6个月以内银承或中兵保兑得3分。
                3.9 报价要求
                """;

        List<ProjectScoreDraft> drafts = parserService.parseExtractedText(1001L, "评分标准.doc", text);

        assertThat(drafts).hasSize(7);
        assertThat(drafts).extracting(ProjectScoreDraft::getCategory)
                .containsExactly("business", "business", "business", "technical", "technical", "price", "price");
        assertThat(drafts).extracting(ProjectScoreDraft::getScoreItemTitle)
                .contains("供应商综合实力", "供应商综合实力（子项2）", "供应商资质", "整体方案", "平台系统对接实施方案", "品类折扣率");
        assertThat(drafts).extracting(ProjectScoreDraft::getScoreValueText)
                .contains("2分", "6分", "15分", "25分");
        assertThat(drafts.get(0).getGeneratedTaskTitle()).contains("供应商综合实力");
        assertThat(drafts.get(0).getSuggestedDeliverables()).contains("响应说明材料");
    }

    @Test
    void parse_ShouldReadDocxAndExtractScoreDrafts() throws Exception {
        byte[] docxBytes = buildTableDocx();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "评分标准.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                docxBytes
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
