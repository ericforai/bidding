// Input: export repositories, DTOs, and support services
// Output: Excel Export business service operations and export metadata
// Pos: Service/业务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.export.service;

import com.xiyu.bid.config.ExportConfig;
import com.xiyu.bid.entity.Tender;
import com.xiyu.bid.repository.TenderRepository;
import com.xiyu.bid.entity.Project;
import com.xiyu.bid.repository.ProjectRepository;
import com.xiyu.bid.entity.Qualification;
import com.xiyu.bid.repository.QualificationRepository;
import com.xiyu.bid.entity.Case;
import com.xiyu.bid.repository.CaseRepository;
import com.xiyu.bid.entity.Template;
import com.xiyu.bid.repository.TemplateRepository;
import com.xiyu.bid.audit.service.IAuditLogService;
import com.xiyu.bid.service.ProjectAccessScopeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Excel Export Service with size limits, audit logging, and pagination support.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelExportService {

    private final TenderRepository tenderRepository;
    private final ProjectRepository projectRepository;
    private final QualificationRepository qualificationRepository;
    private final CaseRepository caseRepository;
    private final TemplateRepository templateRepository;
    private final ExportConfig exportConfig;
    private final IAuditLogService auditLogService;
    private final ProjectAccessScopeService projectAccessScopeService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_ONLY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int DEFAULT_PAGE_SIZE = 1000;

    /**
     * Executor service for timeout-protected export queries.
     * Using a cached thread pool with bounded threads to prevent resource exhaustion.
     */
    private final ExecutorService exportExecutor = Executors.newFixedThreadPool(
        2,  // Limited concurrent exports
        r -> {
            Thread t = new Thread(r);
            t.setName("export-worker-" + t.getId());
            t.setDaemon(true);  // Don't block JVM shutdown
            return t;
        }
    );

    /**
     * Export data to Excel with size limits, audit logging, and query timeout protection.
     *
     * @param dataType The type of data to export (tenders, projects, qualifications, cases, templates)
     * @param filePath The path where the Excel file will be saved
     * @param paramsJson Optional JSON parameters for filtering
     * @param userId The ID of the user performing the export
     * @return The size of the exported file in bytes
     * @throws IllegalArgumentException if data type is null or record count exceeds limit
     * @throws RuntimeException if export fails or times out
     */
    public long exportToExcel(String dataType, Path filePath, String paramsJson, Long userId) {
        return exportToExcelWithResult(dataType, filePath, paramsJson, userId).fileSize();
    }

    /**
     * Export data to Excel and return generated file metadata.
     *
     * @param dataType The type of data to export (tenders, projects, qualifications, cases, templates)
     * @param filePath The path where the Excel file will be saved
     * @param paramsJson Optional JSON parameters for filtering
     * @param userId The ID of the user performing the export
     * @return File size and visible record count for the exported data
     * @throws IllegalArgumentException if data type is null or record count exceeds limit
     * @throws RuntimeException if export fails or times out
     */
    public ExportFileResult exportToExcelWithResult(String dataType, Path filePath, String paramsJson, Long userId) {
        if (dataType == null) {
            throw new IllegalArgumentException("Export type cannot be null");
        }

        // Validate file path to prevent directory traversal
        // Check the original path for ".." before normalization
        String originalPath = filePath.toString();
        if (originalPath.contains("..")) {
            throw new IllegalArgumentException("Invalid file path: path traversal not allowed");
        }
        // Ensure the path is within the allowed temp directory
        String normalizedPath = filePath.normalize().toString();
        String tempDir = System.getProperty("java.io.tmpdir");
        if (!normalizedPath.startsWith(tempDir) && !normalizedPath.startsWith("/tmp/") && !normalizedPath.startsWith("/var/folders/")) {
            // For test purposes, also allow direct /tmp paths
            if (!normalizedPath.startsWith("/tmp")) {
                throw new IllegalArgumentException("Invalid file path: export must be to temp directory");
            }
        }

        long startTime = System.currentTimeMillis();
        int recordCount = 0;
        long fileSize = 0;

        try {
            byte[] data;
            ExportResult result;

            // Execute export with query timeout protection
            Callable<ExportResult> exportTask = () -> {
                return switch (dataType) {
                    case "tenders" -> exportTendersWithLimit();
                    case "projects" -> exportProjectsWithLimit();
                    case "qualifications" -> exportQualificationsWithLimit();
                    case "cases" -> exportCasesWithLimit();
                    case "templates" -> exportTemplatesWithLimit();
                    default -> throw new IllegalArgumentException("Unsupported export type: " + dataType);
                };
            };

            Future<ExportResult> future = exportExecutor.submit(exportTask);
            result = future.get(exportConfig.getQueryTimeoutSeconds(), TimeUnit.SECONDS);
            data = result.data();
            recordCount = result.recordCount();

            // Validate file size
            if (data.length > exportConfig.getMaxFileSizeBytes()) {
                throw new IllegalArgumentException(
                    "Export file size exceeds maximum allowed size of " +
                    (exportConfig.getMaxFileSizeBytes() / 1024 / 1024) + "MB"
                );
            }

            java.nio.file.Files.write(filePath, data);
            fileSize = data.length;

            // Log successful export
            logExport(userId, dataType, recordCount, fileSize, true, null,
                System.currentTimeMillis() - startTime);

            return new ExportFileResult(fileSize, recordCount);

        } catch (TimeoutException e) {
            String errorMsg = String.format(
                "Export query exceeded timeout limit of %d seconds. " +
                "Please refine your export filters or contact admin.",
                exportConfig.getQueryTimeoutSeconds()
            );
            logExport(userId, dataType, recordCount, fileSize, false, errorMsg,
                System.currentTimeMillis() - startTime);
            throw new RuntimeException(errorMsg, e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            String errorMsg = cause != null ? cause.getMessage() : e.getMessage();
            logExport(userId, dataType, recordCount, fileSize, false, errorMsg,
                System.currentTimeMillis() - startTime);
            // Preserve the original exception type for proper exception handling
            if (cause instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) cause;
            } else if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause != null) {
                throw new RuntimeException("Failed to export data to Excel: " + errorMsg, cause);
            } else {
                throw new RuntimeException("Failed to export data to Excel: " + errorMsg, e);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logExport(userId, dataType, recordCount, fileSize, false, "Export interrupted",
                System.currentTimeMillis() - startTime);
            throw new RuntimeException("Export was interrupted", e);
        } catch (IOException e) {
            logExport(userId, dataType, recordCount, fileSize, false, e.getMessage(),
                System.currentTimeMillis() - startTime);
            throw new RuntimeException("Failed to export data to Excel: " + e.getMessage(), e);
        }
    }

    /**
     * Legacy method for backward compatibility. Delegates to the new method with null userId.
     * @deprecated Use {@link #exportToExcel(String, Path, String, Long)} instead
     */
    @Deprecated
    public long exportToExcel(String dataType, Path filePath, String paramsJson) {
        return exportToExcel(dataType, filePath, paramsJson, null);
    }

    public String getExportFileName(String dataType) {
        String baseName = switch (dataType) {
            case "tenders" -> "标讯列表";
            case "projects" -> "项目列表";
            case "qualifications" -> "资质列表";
            case "cases" -> "案例列表";
            case "templates" -> "模板列表";
            default -> "导出数据";
        };
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return baseName + "_" + timestamp + ".xlsx";
    }

    /**
     * Export tenders with pagination support and record limit.
     */
    private ExportResult exportTendersWithLimit() throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("标讯列表");
            CellStyle headerStyle = createHeaderStyle(workbook);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "标题", "来源", "预算金额", "截止日期", "状态", "AI评分", "风险等级"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            int recordCount = 0;
            int pageNumber = 0;
            boolean hasMoreData = true;
            Set<Long> exportableTenderIds = exportableTenderIds();

            while (hasMoreData && recordCount < exportConfig.getMaxRecords()) {
                Pageable pageable = PageRequest.of(pageNumber, DEFAULT_PAGE_SIZE);
                Page<Tender> page = tenderRepository.findAll(pageable);

                for (Tender tender : page.getContent()) {
                    if (recordCount >= exportConfig.getMaxRecords()) {
                        break;
                    }
                    if (!canExportTender(tender, exportableTenderIds)) {
                        continue;
                    }
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(tender.getId() != null ? tender.getId() : 0);
                    row.createCell(1).setCellValue(safeString(tender.getTitle()));
                    row.createCell(2).setCellValue(safeString(tender.getSource()));
                    row.createCell(3).setCellValue(tender.getBudget() != null ? tender.getBudget().doubleValue() : 0);
                    row.createCell(4).setCellValue(formatDateTime(tender.getDeadline()));
                    row.createCell(5).setCellValue(tender.getStatus() != null ? tender.getStatus().name() : "");
                    row.createCell(6).setCellValue(tender.getAiScore() != null ? tender.getAiScore() : 0);
                    row.createCell(7).setCellValue(tender.getRiskLevel() != null ? tender.getRiskLevel().name() : "");
                    recordCount++;
                }

                hasMoreData = page.hasNext();
                pageNumber++;
            }

            // Check if we hit the limit
            if (hasMoreData && recordCount >= exportConfig.getMaxRecords()) {
                log.warn("Export limit reached for tenders: {} records exported, more data available", recordCount);
            }

            autoSizeColumns(sheet, headers.length);
            workbook.write(out);
            return new ExportResult(out.toByteArray(), recordCount);
        }
    }

    /**
     * Export projects with pagination support and record limit.
     */
    private ExportResult exportProjectsWithLimit() throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("项目列表");
            CellStyle headerStyle = createHeaderStyle(workbook);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "项目名称", "关联标讯ID", "状态", "项目经理ID", "开始日期", "结束日期"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            int recordCount = 0;
            int pageNumber = 0;
            boolean hasMoreData = true;
            Set<Long> exportableProjectIds = exportableProjectIds();

            while (hasMoreData && recordCount < exportConfig.getMaxRecords()) {
                Pageable pageable = PageRequest.of(pageNumber, DEFAULT_PAGE_SIZE);
                Page<Project> page = projectRepository.findAll(pageable);

                for (Project project : page.getContent()) {
                    if (recordCount >= exportConfig.getMaxRecords()) {
                        break;
                    }
                    if (!canExportProject(project, exportableProjectIds)) {
                        continue;
                    }
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(project.getId() != null ? project.getId() : 0);
                    row.createCell(1).setCellValue(safeString(project.getName()));
                    row.createCell(2).setCellValue(project.getTenderId() != null ? project.getTenderId() : 0);
                    row.createCell(3).setCellValue(project.getStatus() != null ? project.getStatus().name() : "");
                    row.createCell(4).setCellValue(project.getManagerId() != null ? project.getManagerId() : 0);
                    row.createCell(5).setCellValue(formatDateTime(project.getStartDate()));
                    row.createCell(6).setCellValue(formatDateTime(project.getEndDate()));
                    recordCount++;
                }

                hasMoreData = page.hasNext();
                pageNumber++;
            }

            autoSizeColumns(sheet, headers.length);
            workbook.write(out);
            return new ExportResult(out.toByteArray(), recordCount);
        }
    }

    /**
     * Export qualifications with pagination support and record limit.
     */
    private ExportResult exportQualificationsWithLimit() throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("资质列表");
            CellStyle headerStyle = createHeaderStyle(workbook);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "资质名称", "类型", "级别", "发证日期", "有效期至", "文件路径"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            int recordCount = 0;
            int pageNumber = 0;
            boolean hasMoreData = true;

            while (hasMoreData && recordCount < exportConfig.getMaxRecords()) {
                Pageable pageable = PageRequest.of(pageNumber, DEFAULT_PAGE_SIZE);
                Page<Qualification> page = qualificationRepository.findAll(pageable);

                for (Qualification q : page.getContent()) {
                    if (recordCount >= exportConfig.getMaxRecords()) {
                        break;
                    }
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(q.getId() != null ? q.getId() : 0);
                    row.createCell(1).setCellValue(safeString(q.getName()));
                    row.createCell(2).setCellValue(q.getType() != null ? q.getType().name() : "");
                    row.createCell(3).setCellValue(q.getLevel() != null ? q.getLevel().name() : "");
                    row.createCell(4).setCellValue(formatDateOnly(q.getIssueDate()));
                    row.createCell(5).setCellValue(formatDateOnly(q.getExpiryDate()));
                    row.createCell(6).setCellValue(safeString(q.getFileUrl()));
                    recordCount++;
                }

                hasMoreData = page.hasNext();
                pageNumber++;
            }

            autoSizeColumns(sheet, headers.length);
            workbook.write(out);
            return new ExportResult(out.toByteArray(), recordCount);
        }
    }

    /**
     * Export cases with pagination support and record limit.
     */
    private ExportResult exportCasesWithLimit() throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("案例列表");
            CellStyle headerStyle = createHeaderStyle(workbook);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "标题", "行业", "结果", "金额", "项目日期", "客户名称", "地点", "项目周期"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            int recordCount = 0;
            int pageNumber = 0;
            boolean hasMoreData = true;

            while (hasMoreData && recordCount < exportConfig.getMaxRecords()) {
                Pageable pageable = PageRequest.of(pageNumber, DEFAULT_PAGE_SIZE);
                Page<Case> page = caseRepository.findAll(pageable);

                for (Case c : page.getContent()) {
                    if (recordCount >= exportConfig.getMaxRecords()) {
                        break;
                    }
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(c.getId() != null ? c.getId() : 0);
                    row.createCell(1).setCellValue(safeString(c.getTitle()));
                    row.createCell(2).setCellValue(c.getIndustry() != null ? c.getIndustry().name() : "");
                    row.createCell(3).setCellValue(c.getOutcome() != null ? c.getOutcome().name() : "");
                    row.createCell(4).setCellValue(c.getAmount() != null ? c.getAmount().doubleValue() : 0);
                    row.createCell(5).setCellValue(formatDateOnly(c.getProjectDate()));
                    row.createCell(6).setCellValue(safeString(c.getCustomerName()));
                    row.createCell(7).setCellValue(safeString(c.getLocationName()));
                    row.createCell(8).setCellValue(safeString(c.getProjectPeriod()));
                    recordCount++;
                }

                hasMoreData = page.hasNext();
                pageNumber++;
            }

            autoSizeColumns(sheet, headers.length);
            workbook.write(out);
            return new ExportResult(out.toByteArray(), recordCount);
        }
    }

    /**
     * Export templates with pagination support and record limit.
     */
    private ExportResult exportTemplatesWithLimit() throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("模板列表");
            CellStyle headerStyle = createHeaderStyle(workbook);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "模板名称", "类别", "文件路径", "描述", "当前版本", "文件大小", "创建者ID"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            int recordCount = 0;
            int pageNumber = 0;
            boolean hasMoreData = true;

            while (hasMoreData && recordCount < exportConfig.getMaxRecords()) {
                Pageable pageable = PageRequest.of(pageNumber, DEFAULT_PAGE_SIZE);
                Page<Template> page = templateRepository.findAll(pageable);

                for (Template t : page.getContent()) {
                    if (recordCount >= exportConfig.getMaxRecords()) {
                        break;
                    }
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(t.getId() != null ? t.getId() : 0);
                    row.createCell(1).setCellValue(safeString(t.getName()));
                    row.createCell(2).setCellValue(t.getCategory() != null ? t.getCategory().name() : "");
                    row.createCell(3).setCellValue(safeString(t.getFileUrl()));
                    row.createCell(4).setCellValue(safeString(t.getDescription()));
                    row.createCell(5).setCellValue(safeString(t.getCurrentVersion()));
                    row.createCell(6).setCellValue(safeString(t.getFileSize()));
                    row.createCell(7).setCellValue(t.getCreatedBy() != null ? t.getCreatedBy() : 0);
                    recordCount++;
                }

                hasMoreData = page.hasNext();
                pageNumber++;
            }

            autoSizeColumns(sheet, headers.length);
            workbook.write(out);
            return new ExportResult(out.toByteArray(), recordCount);
        }
    }

    /**
     * Record export operation in audit log.
     */
    private void logExport(Long userId, String dataType, int recordCount, long fileSize,
                          boolean success, String errorMessage, long duration) {
        if (!exportConfig.isAuditEnabled()) {
            return;
        }

        try {
            auditLogService.log(com.xiyu.bid.audit.service.AuditLogService.AuditLogEntry.builder()
                    .userId(userId != null ? String.valueOf(userId) : "system")
                    .action("EXPORT")
                    .entityType(dataType.toUpperCase(java.util.Locale.ROOT))
                    .entityId(null)
                    .description(String.format("Export %s: %d records, %d bytes",
                        dataType, recordCount, fileSize))
                    .success(success)
                    .errorMessage(errorMessage)
                    .build());

            log.info("Export audit: user={}, type={}, records={}, size={}, success={}, duration={}ms",
                userId, dataType, recordCount, fileSize, success, duration);
        } catch (RuntimeException e) {
            log.error("Failed to log export operation: {}", e.getMessage(), e);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
            if (sheet.getColumnWidth(i) < 2000) {
                sheet.setColumnWidth(i, 2000);
            }
            if (sheet.getColumnWidth(i) > 8000) {
                sheet.setColumnWidth(i, 8000);
            }
        }
    }

    private String safeString(String value) {
        return value != null ? value : "";
    }

    private String formatDateTime(LocalDateTime date) {
        return date != null ? date.format(DATE_FORMATTER) : "";
    }

    private String formatDateOnly(LocalDate date) {
        return date != null ? date.format(DATE_ONLY_FORMATTER) : "";
    }

    private Set<Long> exportableProjectIds() {
        if (projectAccessScopeService.currentUserHasAdminAccess()) {
            return null;
        }
        return projectAccessScopeService.filterAccessibleProjects(projectRepository.findAll()).stream()
                .map(Project::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Set<Long> exportableTenderIds() {
        Set<Long> projectIds = exportableProjectIds();
        if (projectIds == null) {
            return null;
        }
        return projectRepository.findAll().stream()
                .filter(project -> project.getId() != null && projectIds.contains(project.getId()))
                .map(Project::getTenderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private boolean canExportProject(Project project, Set<Long> exportableProjectIds) {
        return exportableProjectIds == null
                || project != null && project.getId() != null && exportableProjectIds.contains(project.getId());
    }

    private boolean canExportTender(Tender tender, Set<Long> exportableTenderIds) {
        return exportableTenderIds == null
                || tender != null && tender.getId() != null && exportableTenderIds.contains(tender.getId());
    }

    /**
     * Record to hold export result with data and record count.
     */
    private record ExportResult(byte[] data, int recordCount) {}

    /**
     * Public export metadata returned to API callers.
     */
    public record ExportFileResult(long fileSize, int recordCount) {}
}
