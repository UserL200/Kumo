package com.kumo.dto;

import com.kumo.domain.ExpenseCategory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record BudgetSummaryResponse(
        String month,
        BigDecimal monthlyIncome,
        BigDecimal monthlyRent,
        BigDecimal rentPercentOfIncome,
        BigDecimal expensesTotal,
        Map<ExpenseCategory, BigDecimal> expensesByCategory,
        BigDecimal remaining,
        int onTimeStreakMonths,
        List<ExpenseResponse> expenses) {}