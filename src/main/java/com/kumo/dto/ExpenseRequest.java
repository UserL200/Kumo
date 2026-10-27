package com.kumo.dto;

import com.kumo.domain.ExpenseCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseRequest(
        @NotNull ExpenseCategory category,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        LocalDate date,
        @Size(max = 200) String note) {}