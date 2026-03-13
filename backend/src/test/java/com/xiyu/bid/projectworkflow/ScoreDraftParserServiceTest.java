package com.xiyu.bid.projectworkflow;

import com.xiyu.bid.projectworkflow.entity.ProjectScoreDraft;
import com.xiyu.bid.projectworkflow.service.ScoreDraftGenerationService;
import com.xiyu.bid.projectworkflow.service.ScoreDraftParserService;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScoreDraftParserServiceTest {

    private final ScoreDraftParserService parserService =
            new ScoreDraftParserService(new ScoreDraftGenerationService());

    @Test
    void parse_ShouldExtractRegularScoringTableRowsIntoDrafts() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "score-table.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                buildSampleDocx()
        );

        List<ProjectScoreDraft> drafts = parserService.parse(1001L, file);

        assertThat(drafts).hasSize(3);
        assertThat(drafts).extracting(ProjectScoreDraft::getCategory).containsOnly("other");
        assertThat(drafts).extracting(ProjectScoreDraft::getScoreItemTitle)
                .containsExactly("项目经理资质", "同类项目业绩", "报价得分");
        assertThat(drafts.get(0).getTaskAction()).isEqualTo("准备");
        assertThat(drafts.get(1).getTaskAction()).isEqualTo("整理");
        assertThat(drafts.get(2).getTaskAction()).isEqualTo("复核");
        assertThat(drafts.get(1).getGeneratedTaskTitle()).contains("同类项目业绩");
        assertThat(drafts.get(2).getSuggestedDeliverables()).contains("报价表");
    }

    private byte[] buildSampleDocx() throws Exception {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XWPFTable table = document.createTable(5, 3);
            table.getRow(0).getCell(0).setText("评分项");
            table.getRow(0).getCell(1).setText("评分标准");
            table.getRow(0).getCell(2).setText("分值");

            table.getRow(1).getCell(0).setText("项目经理资质");
            table.getRow(1).getCell(1).setText("提供一级建造师证书得3分");
            table.getRow(1).getCell(2).setText("3分");

            table.getRow(2).getCell(0).setText("同类项目业绩");
            table.getRow(2).getCell(1).setText("每提供1个同类项目业绩得2分，最高6分");
            table.getRow(2).getCell(2).setText("最高6分");

            table.getRow(3).getCell(0).setText("报价得分");
            table.getRow(3).getCell(1).setText("按报价偏差率公式计算得分");
            table.getRow(3).getCell(2).setText("10分");

            table.getRow(4).getCell(0).setText("总分");
            table.getRow(4).getCell(1).setText("100分");
            table.getRow(4).getCell(2).setText("100分");

            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}
