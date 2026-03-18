// Input: resources repositories, DTOs, and support services
// Output: Expense business service operations
// Pos: Service/业务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.resources.service;

import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.resources.dto.ExpenseCreateRequest;
import com.xiyu.bid.resources.dto.ExpenseApproveRequest;
import com.xiyu.bid.resources.dto.ExpenseReturnActionRequest;
import com.xiyu.bid.resources.dto.ExpenseUpdateRequest;
import com.xiyu.bid.resources.entity.Expense;
import com.xiyu.bid.resources.entity.ExpenseApprovalRecord;
import com.xiyu.bid.resources.repository.ExpenseApprovalRecordRepository;
import com.xiyu.bid.resources.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseApprovalRecordRepository expenseApprovalRecordRepository;

    @Transactional
    public Expense createExpense(ExpenseCreateRequest request) {
        validateCreateRequest(request);

        Expense expense = Expense.builder()
                .projectId(request.getProjectId())
                .category(request.getCategory())
                .expenseType(request.getExpenseType())
                .amount(request.getAmount())
                .date(request.getDate())
                .description(request.getDescription())
                .createdBy(request.getCreatedBy())
                .status(Expense.ExpenseStatus.PENDING_APPROVAL)
                .build();

        return expenseRepository.save(expense);
    }

    public Expense getExpenseById(Long id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", String.valueOf(id)));
    }

    public Page<Expense> getAllExpenses(Pageable pageable) {
        return expenseRepository.findAll(pageable);
    }

    public Page<Expense> getExpensesByProjectId(Long projectId, Pageable pageable) {
        return expenseRepository.findByProjectId(projectId, pageable);
    }

    public Page<Expense> getExpensesByCategory(Expense.ExpenseCategory category, Pageable pageable) {
        return expenseRepository.findByCategory(category, pageable);
    }

    public Page<Expense> getExpensesByDateRange(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return expenseRepository.findByDateBetween(startDate, endDate, pageable);
    }

    @Transactional
    public Expense updateExpense(Long id, ExpenseUpdateRequest request) {
        Expense expense = getExpenseById(id);
        expense.updateDetails(request.getCategory(), request.getAmount(), request.getDate(),
                request.getExpenseType(), request.getDescription());
        return expenseRepository.save(expense);
    }

    @Transactional
    public void deleteExpense(Long id) {
        if (!expenseRepository.existsById(id)) {
            throw new ResourceNotFoundException("Expense", String.valueOf(id));
        }
        expenseRepository.deleteById(id);
    }

    public BigDecimal getTotalExpenseByProject(Long projectId) {
        BigDecimal total = expenseRepository.sumAmountByProjectId(projectId);
        return total != null ? total : BigDecimal.ZERO;
    }

    public Map<String, Object> getExpenseStatisticsByProject(Long projectId) {
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalAmount", getTotalExpenseByProject(projectId));
        statistics.put("projectId", projectId);

        for (Expense.ExpenseCategory category : Expense.ExpenseCategory.values()) {
            BigDecimal categoryTotal = expenseRepository.sumAmountByProjectIdAndCategory(projectId, category);
            statistics.put(category.name().toLowerCase(), categoryTotal != null ? categoryTotal : BigDecimal.ZERO);
        }

        return statistics;
    }

    public List<ExpenseApprovalRecord> getApprovalRecords(Long projectId) {
        if (projectId == null) {
            return expenseApprovalRecordRepository.findAll().stream()
                    .sorted((a, b) -> b.getActedAt().compareTo(a.getActedAt()))
                    .toList();
        }

        return expenseRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(Expense::getId)
                .flatMap(expenseId -> expenseApprovalRecordRepository.findByExpenseIdOrderByActedAtDesc(expenseId).stream())
                .sorted((a, b) -> b.getActedAt().compareTo(a.getActedAt()))
                .toList();
    }

    @Transactional
    public Expense approveExpense(Long id, ExpenseApproveRequest request) {
        Expense expense = getExpenseById(id);
        Expense.ExpenseStatus nextStatus = request.getResult() == ExpenseApproveRequest.ApprovalResult.APPROVED
                ? Expense.ExpenseStatus.APPROVED
                : Expense.ExpenseStatus.REJECTED;
        expense.markApproved(request.getApprover(), request.getComment(), nextStatus);

        Expense saved = expenseRepository.save(expense);

        expenseApprovalRecordRepository.save(ExpenseApprovalRecord.builder()
                .expenseId(saved.getId())
                .result(request.getResult() == ExpenseApproveRequest.ApprovalResult.APPROVED
                        ? ExpenseApprovalRecord.ApprovalResult.APPROVED
                        : ExpenseApprovalRecord.ApprovalResult.REJECTED)
                .comment(request.getComment())
                .approver(request.getApprover())
                .actedAt(LocalDateTime.now())
                .build());

        return saved;
    }

    @Transactional
    public Expense requestReturn(Long id, ExpenseReturnActionRequest request) {
        Expense expense = getExpenseById(id);
        expense.requestReturn(request.getActor(), request.getComment());
        return expenseRepository.save(expense);
    }

    @Transactional
    public Expense confirmReturn(Long id, ExpenseReturnActionRequest request) {
        Expense expense = getExpenseById(id);
        expense.confirmReturn(request.getActor(), request.getComment());
        return expenseRepository.save(expense);
    }

    private void validateCreateRequest(ExpenseCreateRequest request) {
        if (request.getProjectId() == null) {
            throw new IllegalArgumentException("Project ID is required");
        }
        if (request.getCategory() == null) {
            throw new IllegalArgumentException("Category is required");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (request.getDate() == null) {
            throw new IllegalArgumentException("Date is required");
        }
        if (request.getDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Date cannot be in the future");
        }
        if (request.getCreatedBy() == null || request.getCreatedBy().trim().isEmpty()) {
            throw new IllegalArgumentException("Created by is required");
        }
    }
}
