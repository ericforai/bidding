package com.xiyu.bid.resources.service;

import com.xiyu.bid.resources.dto.ExpenseCreateRequest;
import com.xiyu.bid.resources.dto.ExpenseUpdateRequest;
import com.xiyu.bid.resources.entity.Expense;
import com.xiyu.bid.resources.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseService {

    private final ExpenseRepository expenseRepository;

    @Transactional
    public Expense createExpense(ExpenseCreateRequest request) {
        // Validation
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

        Expense expense = Expense.builder()
                .projectId(request.getProjectId())
                .category(request.getCategory())
                .amount(request.getAmount())
                .date(request.getDate())
                .description(request.getDescription())
                .createdBy(request.getCreatedBy())
                .build();

        return expenseRepository.save(expense);
    }

    public Expense getExpenseById(Long id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found with id: " + id));
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

        if (request.getCategory() != null) {
            expense.setCategory(request.getCategory());
        }
        if (request.getAmount() != null) {
            if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Amount must be positive");
            }
            expense.setAmount(request.getAmount());
        }
        if (request.getDate() != null) {
            if (request.getDate().isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("Date cannot be in the future");
            }
            expense.setDate(request.getDate());
        }
        if (request.getDescription() != null) {
            expense.setDescription(request.getDescription());
        }

        return expenseRepository.save(expense);
    }

    @Transactional
    public void deleteExpense(Long id) {
        if (!expenseRepository.existsById(id)) {
            throw new RuntimeException("Expense not found with id: " + id);
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
}
