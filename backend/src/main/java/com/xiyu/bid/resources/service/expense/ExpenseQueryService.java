package com.xiyu.bid.resources.service.expense;

import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.resources.dto.ExpenseApprovalRecordDTO;
import com.xiyu.bid.resources.dto.ExpenseDTO;
import com.xiyu.bid.resources.dto.ResourceResponseMapper;
import com.xiyu.bid.resources.entity.Expense;
import com.xiyu.bid.resources.entity.ExpensePaymentRecord;
import com.xiyu.bid.resources.repository.ExpenseApprovalRecordRepository;
import com.xiyu.bid.resources.repository.ExpensePaymentRecordRepository;
import com.xiyu.bid.resources.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExpenseQueryService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseApprovalRecordRepository expenseApprovalRecordRepository;
    private final ExpensePaymentRecordRepository expensePaymentRecordRepository;

    public ExpenseDTO getExpenseById(Long id) {
        return expenseRepository.findById(id)
                .map(expense -> ResourceResponseMapper.toDto(expense, findLatestPayment(expense.getId())))
                .orElseThrow(() -> new ResourceNotFoundException("Expense", String.valueOf(id)));
    }

    public Page<ExpenseDTO> getAllExpenses(Pageable pageable) {
        Page<Expense> expenses = expenseRepository.findAll(pageable);
        return mapExpensesWithLatestPayments(expenses);
    }

    public Page<ExpenseDTO> getExpensesByProjectId(Long projectId, Pageable pageable) {
        Page<Expense> expenses = expenseRepository.findByProjectId(projectId, pageable);
        return mapExpensesWithLatestPayments(expenses);
    }

    public Page<ExpenseDTO> getExpensesByCategory(String category, Pageable pageable) {
        Page<Expense> expenses = expenseRepository.findByCategory(Expense.ExpenseCategory.valueOf(category), pageable);
        return mapExpensesWithLatestPayments(expenses);
    }

    public Page<ExpenseDTO> getExpensesByDateRange(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Page<Expense> expenses = expenseRepository.findByDateBetween(startDate, endDate, pageable);
        return mapExpensesWithLatestPayments(expenses);
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

    public List<ExpenseApprovalRecordDTO> getApprovalRecords(Long projectId) {
        if (projectId == null) {
            return expenseApprovalRecordRepository.findAll().stream()
                    .sorted((a, b) -> b.getActedAt().compareTo(a.getActedAt()))
                    .map(ResourceResponseMapper::toDto)
                    .toList();
        }

        return expenseRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(Expense::getId)
                .flatMap(expenseId -> expenseApprovalRecordRepository.findByExpenseIdOrderByActedAtDesc(expenseId).stream())
                .sorted((a, b) -> b.getActedAt().compareTo(a.getActedAt()))
                .map(ResourceResponseMapper::toDto)
                .toList();
    }

    private ExpensePaymentRecord findLatestPayment(Long expenseId) {
        return expensePaymentRecordRepository.findByExpenseIdOrderByPaidAtDescIdDesc(expenseId).stream()
                .findFirst()
                .orElse(null);
    }

    private Page<ExpenseDTO> mapExpensesWithLatestPayments(Page<Expense> expenses) {
        Map<Long, ExpensePaymentRecord> latestPayments = findLatestPayments(expenses.getContent());
        return expenses.map(expense -> ResourceResponseMapper.toDto(expense, latestPayments.get(expense.getId())));
    }

    private Map<Long, ExpensePaymentRecord> findLatestPayments(List<Expense> expenses) {
        if (expenses.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> expenseIds = expenses.stream()
                .map(Expense::getId)
                .toList();

        Map<Long, ExpensePaymentRecord> latestPayments = new LinkedHashMap<>();
        expensePaymentRecordRepository.findByExpenseIdInOrderByExpenseIdAscPaidAtDescIdDesc(expenseIds)
                .forEach(payment -> latestPayments.putIfAbsent(payment.getExpenseId(), payment));

        return latestPayments;
    }
}
