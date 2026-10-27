package com.kumo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record IncomeRequest(@NotNull @DecimalMin("0.00") BigDecimal monthlyIncome) {}