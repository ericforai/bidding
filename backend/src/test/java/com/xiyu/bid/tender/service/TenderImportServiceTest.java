package com.xiyu.bid.tender.service;

import com.xiyu.bid.tender.dto.TenderDTO;
import com.xiyu.bid.tender.dto.TenderImportResultDTO;
import com.xiyu.bid.tender.dto.TenderRequest;
import jakarta.validation.Validator;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenderImportServiceTest {

    @Mock
    private TenderCommandService tenderCommandService;

    @Mock
    private TenderMapper tenderMapper;

    @Mock
    private Validator validator;

    private TenderImportTemplateBuilder templateBuilder;
    private TenderImportService service;

    @BeforeEach
    void setUp() {
        templateBuilder = new TenderImportTemplateBuilder();
        service = new TenderImportService(tenderCommandService, tenderMapper, templateBuilder, validator);
    }

    @Test
    @DisplayName("generateTemplate 输出包含「标讯导入」与「字典参考」两张 sheet")
    void generateTemplateProducesTwoSheets() throws Exception {
        byte[] bytes = service.generateTemplate();
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(2);
            assertThat(workbook.getSheetName(0)).isEqualTo("标讯导入");
            assertThat(workbook.getSheetName(1)).isEqualTo("字典参考");
            Row header = workbook.getSheetAt(0).getRow(0);
            for (int i = 0; i < TenderImportService.HEADERS.length; i++) {
                assertThat(header.getCell(i).getStringCellValue()).isEqualTo(TenderImportService.HEADERS[i]);
            }
        }
    }

    @Test
    @DisplayName("空文件应当被拒绝并提示")
    void emptyFileIsRejected() {
        MockMultipartFile empty = new MockMultipartFile("file", "x.xlsx", "application/octet-stream", new byte[0]);
        assertThatThrownBy(() -> service.importFromExcel(empty))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("请上传");
    }

    @Test
    @DisplayName("非 .xlsx 扩展名应当被拒绝")
    void nonXlsxExtensionIsRejected() {
        MockMultipartFile csv = new MockMultipartFile(
                "file", "tenders.csv", "text/csv", "a,b,c".getBytes());
        assertThatThrownBy(() -> service.importFromExcel(csv))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("仅支持 .xlsx");
    }

    @Test
    @DisplayName("表头错位应当抛出「模板表头不匹配」")
    void headerMismatchIsRejected() throws Exception {
        byte[] bytes = buildWorkbookWithHeaders(new String[]{"错误表头"}, new String[][]{});
        MockMultipartFile bad = new MockMultipartFile(
                "file", "bad.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);
        assertThatThrownBy(() -> service.importFromExcel(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("模板表头不匹配");
    }

    @Test
    @DisplayName("任一行非法应当全量回滚，不调用 createTender")
    void anyInvalidRowTriggersRollback() throws Exception {
        when(validator.validate(any(TenderRequest.class))).thenReturn(Collections.emptySet());

        // 客户类型不在白名单 -> 校验失败
        String[] row = exampleRow();
        row[8] = "未知类型";
        byte[] bytes = buildWorkbookWithHeaders(TenderImportService.HEADERS, new String[][]{row});

        MockMultipartFile file = new MockMultipartFile(
                "file", "import.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        assertThatThrownBy(() -> service.importFromExcel(file))
                .isInstanceOf(TenderImportRollbackException.class)
                .satisfies(ex -> {
                    TenderImportResultDTO result = ((TenderImportRollbackException) ex).getResult();
                    assertThat(result.getTotalRows()).isEqualTo(1);
                    assertThat(result.getSuccessCount()).isZero();
                    assertThat(result.getFailureCount()).isEqualTo(1);
                    assertThat(result.getErrors()).hasSize(1);
                    assertThat(result.getErrors().get(0).field()).isEqualTo("customerType");
                });

        verify(tenderCommandService, never()).createTender(any());
    }

    @Test
    @DisplayName("全部行合法时应当依次创建并返回成功汇总")
    void allValidRowsAreImported() throws Exception {
        when(validator.validate(any(TenderRequest.class))).thenReturn(Collections.emptySet());
        when(tenderMapper.toDTO(any(TenderRequest.class))).thenReturn(new TenderDTO());

        byte[] bytes = buildWorkbookWithHeaders(TenderImportService.HEADERS, new String[][]{
                exampleRow(),
                exampleRow()
        });
        MockMultipartFile file = new MockMultipartFile(
                "file", "import.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        TenderImportResultDTO result = service.importFromExcel(file);
        assertThat(result.getTotalRows()).isEqualTo(2);
        assertThat(result.getSuccessCount()).isEqualTo(2);
        assertThat(result.getFailureCount()).isZero();
        assertThat(result.getErrors()).isEmpty();
        verify(tenderCommandService, times(2)).createTender(any());
    }

    private String[] exampleRow() {
        return new String[]{
                "测试标讯标题",
                "测试招标代理",
                "测试采购单位",
                "北京",
                "2026-12-31 17:00",
                "2026-12-25 09:30",
                "张三",
                "13800138000",
                "央企集团",
                "A",
                "1500000",
                "数据中心",
                "示例描述",
                "MRO 工具,机柜"
        };
    }

    private byte[] buildWorkbookWithHeaders(String[] headers, String[][] dataRows) throws Exception {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("标讯导入");
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
            }
            for (int r = 0; r < dataRows.length; r++) {
                Row row = sheet.createRow(r + 1);
                String[] values = dataRows[r];
                for (int c = 0; c < values.length; c++) {
                    Cell cell = row.createCell(c);
                    cell.setCellValue(values[c]);
                }
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
