package com.xiyu.bid.resources.controller;

import com.xiyu.bid.annotation.Auditable;
import com.xiyu.bid.config.PaginationConstants;
import com.xiyu.bid.dto.ApiResponse;
import com.xiyu.bid.resources.dto.ExpenseApproveRequest;
import com.xiyu.bid.resources.dto.ExpenseCreateRequest;
import com.xiyu.bid.resources.dto.ExpenseReturnActionRequest;
import com.xiyu.bid.resources.dto.ExpenseUpdateRequest;
import com.xiyu.bid.resources.entity.Expense;
import com.xiyu.bid.resources.entity.ExpenseApprovalRecord;
import com.xiyu.bid.resources.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resources/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Auditable(action = "CREATE", entityType = "Expense", description = "Create expense record")
    public ResponseEntity<ApiResponse<Expense>> createExpense(@Valid @RequestBody ExpenseCreateRequest request) {
        Expense expense = expenseService.createExpense(request);
        return ResponseEntity.ok(ApiResponse.success("Expense created successfully", expense));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Expense>> getExpenseById(@PathVariable Long id) {
        Expense expense = expenseService.getExpenseById(id);
        return ResponseEntity.ok(ApiResponse.success(expense));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Page<Expense>>> getAllExpenses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        if (size > PaginationConstants.MAX_PAGE_SIZE) {
            size = PaginationConstants.MAX_PAGE_SIZE;
        }

        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Expense> expenses = expenseService.getAllExpenses(pageable);
        return ResponseEntity.ok(ApiResponse.success(expenses));
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Page<Expense>>> getExpensesByProjectId(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (size > PaginationConstants.MAX_PAGE_SIZE) {
            size = PaginationConstants.MAX_PAGE_SIZE;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("date").descending());
        Page<Expense> expenses = expenseService.getExpensesByProjectId(projectId, pageable);
        return ResponseEntity.ok(ApiResponse.success(expenses));
    }

    @GetMapping("/category/{category}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Page<Expense>>> getExpensesByCategory(
            @PathVariable Expense.ExpenseCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (size > PaginationConstants.MAX_PAGE_SIZE) {
            size = PaginationConstants.MAX_PAGE_SIZE;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("date").descending());
        Page<Expense> expenses = expenseService.getExpensesByCategory(category, pageable);
        return ResponseEntity.ok(ApiResponse.success(expenses));
    }

    @GetMapping("/date-range")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Page<Expense>>> getExpensesByDateRange(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (size > PaginationConstants.MAX_PAGE_SIZE) {
            size = PaginationConstants.MAX_PAGE_SIZE;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("date").descending());
        Page<Expense> expenses = expenseService.getExpensesByDateRange(startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success(expenses));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Auditable(action = "UPDATE", entityType = "Expense", description = "Update expense record")
    public ResponseEntity<ApiResponse<Expense>> updateExpense(
            @PathVariable Long id,
            @Valid @RequestBody ExpenseUpdateRequest request) {

        Expense expense = expenseService.updateExpense(id, request);
        return ResponseEntity.ok(ApiResponse.success("Expense updated successfully", expense));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Auditable(action = "DELETE", entityType = "Expense", description = "Delete expense record")
    public ResponseEntity<ApiResponse<Void>> deleteExpense(@PathVariable Long id) {
        expenseService.deleteExpense(id);
        return ResponseEntity.ok(ApiResponse.success("Expense deleted successfully", null));
    }

    @GetMapping("/project/{projectId}/total")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<BigDecimal>> getTotalExpenseByProject(@PathVariable Long projectId) {
        BigDecimal total = expenseService.getTotalExpenseByProject(projectId);
        return ResponseEntity.ok(ApiResponse.success(total));
    }

    @GetMapping("/project/{projectId}/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getExpenseStatistics(@PathVariable Long projectId) {
        Map<String, Object> statistics = expenseService.getExpenseStatisticsByProject(projectId);
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }

    @GetMapping("/approval-records")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<List<ExpenseApprovalRecord>>> getApprovalRecords(
            @RequestParam(required = false) Long projectId) {
        List<ExpenseApprovalRecord> records = expenseService.getApprovalRecords(projectId);
        return ResponseEntity.ok(ApiResponse.success(records));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Auditable(action = "APPROVE", entityType = "Expense", description = "Approve or reject expense")
    public ResponseEntity<ApiResponse<Expense>> approveExpense(
            @PathVariable Long id,
            @Valid @RequestBody ExpenseApproveRequest request) {
        Expense expense = expenseService.approveExpense(id, request);
        return ResponseEntity.ok(ApiResponse.success("Expense approval action completed", expense));
    }

    @PostMapping("/{id}/return-request")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Auditable(action = "RETURN_REQUEST", entityType = "Expense", description = "Request expense return")
    public ResponseEntity<ApiResponse<Expense>> requestReturn(
            @PathVariable Long id,
            @Valid @RequestBody ExpenseReturnActionRequest request) {
        Expense expense = expenseService.requestReturn(id, request);
        return ResponseEntity.ok(ApiResponse.success("Expense return requested", expense));
    }

    @PostMapping("/{id}/confirm-return")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Auditable(action = "CONFIRM_RETURN", entityType = "Expense", description = "Confirm expense return")
    public ResponseEntity<ApiResponse<Expense>> confirmReturn(
            @PathVariable Long id,
            @Valid @RequestBody ExpenseReturnActionRequest request) {
        Expense expense = expenseService.confirmReturn(id, request);
        return ResponseEntity.ok(ApiResponse.success("Expense return confirmed", expense));
    }
}
