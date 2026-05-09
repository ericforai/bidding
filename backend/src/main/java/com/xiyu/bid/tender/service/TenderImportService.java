// Input: MultipartFile（.xlsx）+ 上传用户上下文
// Output: TenderImportResultDTO（成功）/ TenderImportRollbackException（任一行不合法）
// Pos: service/标讯批量导入用例
// 维护声明: HEADERS / REGIONS / CUSTOMER_TYPES / PRIORITIES 与 TenderImportTemplateBuilder 共享，调整需同步前端 BulkImportDialog 与说明文案。

package com.xiyu.bid.tender.service;

import com.xiyu.bid.tender.dto.TenderImportResultDTO;
import com.xiyu.bid.tender.dto.TenderImportResultDTO.RowError;
import com.xiyu.bid.tender.dto.TenderRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 标讯批量导入：模板生成 + Excel 解析 + 单条入库 + 全量回滚。
 * <p>校验策略：先解析整张表 → 累计行级错误 → 错误为空时逐条 {@link TenderCommandService#createTender(com.xiyu.bid.tender.dto.TenderDTO)}；
 * 否则抛 {@link TenderImportRollbackException} 触发整批回滚。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenderImportService {

    static final String[] HEADERS = {
            "标讯标题*", "招标机构*", "采购单位*", "总部所在地*",
            "报名截止时间*", "开标时间*", "联系人*", "联系方式*",
            "客户类型*", "优先级*",
            "预算（元）", "行业", "描述", "标签（多个用逗号分隔）"
    };

    static final List<String> CUSTOMER_TYPES = List.of("央企集团", "国有集团", "KA 客户");
    static final List<String> PRIORITIES = List.of("S", "A", "B", "C");
    static final List<String> REGIONS = List.of(
            "北京", "天津", "河北", "山西", "内蒙古", "辽宁", "吉林", "黑龙江", "上海", "江苏",
            "浙江", "安徽", "福建", "江西", "山东", "河南", "湖北", "湖南", "广东", "广西",
            "海南", "重庆", "四川", "贵州", "云南", "西藏", "陕西", "甘肃", "青海", "宁夏",
            "新疆", "台湾", "香港", "澳门"
    );

    private static final int MAX_ROWS = 500;
    private static final long MAX_FILE_BYTES = 5L * 1024 * 1024;

    private static final List<DateTimeFormatter> DATETIME_PATTERNS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
    );
    private static final List<DateTimeFormatter> DATE_PATTERNS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd")
    );

    private final TenderCommandService tenderCommandService;
    private final TenderMapper tenderMapper;
    private final TenderImportTemplateBuilder templateBuilder;
    private final Validator validator;

    public byte[] generateTemplate() {
        return templateBuilder.build();
    }

    @Transactional
    public TenderImportResultDTO importFromExcel(MultipartFile file) {
        validateFile(file);
        List<RowError> errors = new ArrayList<>();
        List<TenderRequest> rows = new ArrayList<>();
        int totalRows;

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            validateHeader(sheet);
            totalRows = collectRows(sheet, rows, errors);
        } catch (IOException e) {
            throw new IllegalArgumentException("Excel 解析失败：" + e.getMessage(), e);
        }

        if (!errors.isEmpty()) {
            log.info("标讯批量导入校验未通过 totalRows={} failureCount={}", totalRows, errors.size());
            throw new TenderImportRollbackException(TenderImportResultDTO.builder()
                    .totalRows(totalRows)
                    .successCount(0)
                    .failureCount(errors.size())
                    .errors(List.copyOf(errors))
                    .build());
        }

        rows.forEach(req -> tenderCommandService.createTender(tenderMapper.toDTO(req)));
        log.info("标讯批量导入完成 totalRows={}", totalRows);
        return TenderImportResultDTO.builder()
                .totalRows(totalRows)
                .successCount(totalRows)
                .failureCount(0)
                .errors(List.of())
                .build();
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请上传导入文件");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new IllegalArgumentException("导入文件不能超过 5MB");
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new IllegalArgumentException("仅支持 .xlsx 模板，请使用下载的模板");
        }
    }

    private void validateHeader(Sheet sheet) {
        Row header = sheet == null ? null : sheet.getRow(0);
        if (header == null) {
            throw new IllegalArgumentException("模板表头不匹配，请使用最新模板");
        }
        for (int i = 0; i < HEADERS.length; i++) {
            String actual = readString(header.getCell(i));
            if (actual == null || !HEADERS[i].equals(actual)) {
                throw new IllegalArgumentException("模板表头不匹配，请使用最新模板");
            }
        }
    }

    private int collectRows(Sheet sheet, List<TenderRequest> rows, List<RowError> errors) {
        int last = sheet.getLastRowNum();
        int count = 0;
        for (int i = 1; i <= last; i++) {
            Row row = sheet.getRow(i);
            if (isBlankRow(row)) continue;
            count++;
            if (count > MAX_ROWS) {
                throw new IllegalArgumentException("单次导入最多 " + MAX_ROWS + " 行");
            }
            int displayRow = i + 1;
            try {
                TenderRequest request = parseRow(row);
                rows.add(request);
                runValidation(displayRow, request, errors);
            } catch (IllegalArgumentException e) {
                errors.add(new RowError(displayRow, "row", e.getMessage()));
            }
        }
        return count;
    }

    private TenderRequest parseRow(Row row) {
        TenderRequest req = new TenderRequest();
        req.setTitle(readString(row.getCell(0)));
        req.setTenderAgency(readString(row.getCell(1)));
        req.setPurchaserName(readString(row.getCell(2)));
        req.setRegion(readString(row.getCell(3)));
        req.setDeadline(readDateTime(row.getCell(4), "报名截止时间"));
        req.setBidOpeningTime(readDateTime(row.getCell(5), "开标时间"));
        req.setContactName(readString(row.getCell(6)));
        req.setContactPhone(readString(row.getCell(7)));
        req.setCustomerType(readString(row.getCell(8)));
        req.setPriority(readString(row.getCell(9)));
        req.setBudget(readBigDecimal(row.getCell(10)));
        req.setIndustry(readString(row.getCell(11)));
        req.setDescription(readString(row.getCell(12)));
        String tags = readString(row.getCell(13));
        if (tags != null) {
            req.setTags(Arrays.stream(tags.split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).toList());
        }
        req.setSource("manual");
        req.setPublishDate(LocalDate.now());
        return req;
    }

    private void runValidation(int displayRow, TenderRequest request, List<RowError> errors) {
        Set<ConstraintViolation<TenderRequest>> violations = validator.validate(request);
        for (ConstraintViolation<TenderRequest> v : violations) {
            errors.add(new RowError(displayRow, v.getPropertyPath().toString(), v.getMessage()));
        }
        if (request.getCustomerType() != null && !CUSTOMER_TYPES.contains(request.getCustomerType())) {
            errors.add(new RowError(displayRow, "customerType",
                    "客户类型必须是：" + String.join(" / ", CUSTOMER_TYPES)));
        }
        if (request.getPriority() != null && !PRIORITIES.contains(request.getPriority())) {
            errors.add(new RowError(displayRow, "priority", "优先级必须是 S/A/B/C 之一"));
        }
        if (request.getRegion() != null && !REGIONS.contains(request.getRegion())) {
            errors.add(new RowError(displayRow, "region", "总部所在地不在支持范围内"));
        }
    }

    private boolean isBlankRow(Row row) {
        if (row == null) return true;
        for (int i = 0; i < HEADERS.length; i++) {
            String value = readString(row.getCell(i));
            if (value != null && !value.isBlank()) return false;
        }
        return true;
    }

    private String readString(Cell cell) {
        if (cell == null) return null;
        String raw = switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> formatNumeric(cell);
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> safeFormulaText(cell);
            default -> null;
        };
        if (raw == null) return null;
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String formatNumeric(Cell cell) {
        if (DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toString();
        }
        double d = cell.getNumericCellValue();
        if (d == Math.floor(d) && !Double.isInfinite(d) && Math.abs(d) < 1e15) {
            return String.valueOf((long) d);
        }
        return BigDecimal.valueOf(d).stripTrailingZeros().toPlainString();
    }

    private String safeFormulaText(Cell cell) {
        try {
            return cell.getStringCellValue();
        } catch (IllegalStateException ex) {
            return BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
        }
    }

    private LocalDateTime readDateTime(Cell cell, String label) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue();
        }
        String text = readString(cell);
        if (text == null) return null;
        for (DateTimeFormatter fmt : DATETIME_PATTERNS) {
            try { return LocalDateTime.parse(text, fmt); } catch (DateTimeParseException ignored) { /* try next */ }
        }
        for (DateTimeFormatter fmt : DATE_PATTERNS) {
            try { return LocalDate.parse(text, fmt).atTime(23, 59, 59); } catch (DateTimeParseException ignored) { /* try next */ }
        }
        throw new IllegalArgumentException(label + "格式错误：" + text + "（推荐 yyyy-MM-dd HH:mm:ss）");
    }

    private BigDecimal readBigDecimal(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && !DateUtil.isCellDateFormatted(cell)) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }
        String text = readString(cell);
        if (text == null) return null;
        try {
            return new BigDecimal(text.replace(",", ""));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("预算金额格式错误：" + text);
        }
    }
}
