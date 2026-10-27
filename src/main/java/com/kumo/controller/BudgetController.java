package com.kumo.controller;

import com.kumo.dto.BudgetSummaryResponse;
import com.kumo.dto.ExpenseRequest;
import com.kumo.dto.ExpenseResponse;
import com.kumo.dto.IncomeRequest;
import com.kumo.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/budget")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping("/summary")
    public BudgetSummaryResponse summary(
            Authentication auth,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return budgetService.getSummary(auth.getName(), month);
    }

    @PutMapping("/income")
    public Map<String, BigDecimal> setIncome(Authentication auth, @Valid @RequestBody IncomeRequest request) {
        return Map.of("monthlyIncome", budgetService.setIncome(auth.getName(), request.monthlyIncome()));
    }

    @PostMapping("/expenses")
    public ResponseEntity<ExpenseResponse> addExpense(Authentication auth, @Valid @RequestBody ExpenseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(budgetService.addExpense(auth.getName(), request));
    }

    @DeleteMapping("/expenses/{id}")
    public ResponseEntity<Void> deleteExpense(Authentication auth, @PathVariable Long id) {
        budgetService.deleteExpense(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }
}