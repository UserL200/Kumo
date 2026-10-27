package com.kumo.service;

import com.kumo.domain.BudgetExpense;
import com.kumo.domain.BudgetProfile;
import com.kumo.domain.ExpenseCategory;
import com.kumo.domain.PaymentStatus;
import com.kumo.domain.Property;
import com.kumo.domain.User;
import com.kumo.dto.BudgetSummaryResponse;
import com.kumo.dto.ExpenseRequest;
import com.kumo.dto.ExpenseResponse;
import com.kumo.exception.ResourceNotFoundException;
import com.kumo.repository.BudgetExpenseRepository;
import com.kumo.repository.BudgetProfileRepository;
import com.kumo.repository.PropertyRepository;
import com.kumo.repository.RentRecordRepository;
import com.kumo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final UserRepository userRepository;
    private final BudgetProfileRepository profileRepository;
    private final BudgetExpenseRepository expenseRepository;
    private final PropertyRepository propertyRepository;
    private final RentRecordRepository rentRecordRepository;

    @Transactional
    public BigDecimal setIncome(String email, BigDecimal income) {
        User user = requireUser(email);
        BudgetProfile profile = profileRepository.findByUserId(user.getId())
                .orElseGet(() -> BudgetProfile.builder().user(user).build());
        profile.setMonthlyIncome(income);
        profile.setUpdatedAt(LocalDateTime.now());
        return profileRepository.save(profile).getMonthlyIncome();
    }

    @Transactional
    public ExpenseResponse addExpense(String email, ExpenseRequest request) {
        User user = requireUser(email);
        LocalDate date = request.date() != null ? request.date() : LocalDate.now();

        BudgetExpense expense = BudgetExpense.builder()
                .user(user)
                .category(request.category())
                .amount(request.amount())
                .month(date.withDayOfMonth(1))
                .note(request.note() != null ? request.note().trim() : null)
                .build();

        return toResponse(expenseRepository.save(expense));
    }

    @Transactional
    public void deleteExpense(String email, Long expenseId) {
        User user = requireUser(email);
        BudgetExpense expense = expenseRepository.findByIdAndUserId(expenseId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        expenseRepository.delete(expense);
    }

    @Transactional(readOnly = true)
    public BudgetSummaryResponse getSummary(String email, YearMonth month) {
        User user = requireUser(email);
        YearMonth target = month != null ? month : YearMonth.now();

        BigDecimal income = profileRepository.findByUserId(user.getId())
                .map(BudgetProfile::getMonthlyIncome)
                .orElse(BigDecimal.ZERO);

        BigDecimal rent = propertyRepository.findByTenantId(user.getId()).stream()
                .filter(Property::isActive)
                .map(Property::getMonthlyRent)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<BudgetExpense> expenses = expenseRepository
                .findByUserIdAndMonthOrderByCreatedAtDesc(user.getId(), target.atDay(1));

        BigDecimal expensesTotal = expenses.stream()
                .map(BudgetExpense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<ExpenseCategory, BigDecimal> byCategory = new EnumMap<>(ExpenseCategory.class);
        for (BudgetExpense e : expenses) {
            byCategory.merge(e.getCategory(), e.getAmount(), BigDecimal::add);
        }

        BigDecimal rentPercent = income.signum() > 0
                ? rent.multiply(BigDecimal.valueOf(100)).divide(income, 1, RoundingMode.HALF_UP)
                : null;

        return new BudgetSummaryResponse(
                target.toString(),
                income,
                rent,
                rentPercent,
                expensesTotal,
                byCategory,
                income.subtract(rent).subtract(expensesTotal),
                computeStreak(user.getId()),
                expenses.stream().map(this::toResponse).toList());
    }

    // Consecutive months with a CONFIRMED payment, counted back from this month.
    // If this month has no confirmed payment yet, we start from last month so
    // the streak doesn't reset to zero on the 1st.
    private int computeStreak(Long tenantId) {
        Set<YearMonth> paidMonths = rentRecordRepository
                .findByTenantIdOrderByPaymentDateDesc(tenantId).stream()
                .filter(r -> r.getStatus() == PaymentStatus.CONFIRMED)
                .map(r -> YearMonth.from(r.getPaymentDate()))
                .collect(Collectors.toSet());

        YearMonth cursor = YearMonth.now();
        if (!paidMonths.contains(cursor)) cursor = cursor.minusMonths(1);

        int streak = 0;
        while (paidMonths.contains(cursor)) {
            streak++;
            cursor = cursor.minusMonths(1);
        }
        return streak;
    }

    private ExpenseResponse toResponse(BudgetExpense e) {
        return new ExpenseResponse(e.getId(), e.getCategory(), e.getAmount(), e.getMonth(), e.getNote());
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}