package com.kumo.dto;

import com.kumo.domain.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseResponse(Long id, ExpenseCategory category, BigDecimal amount,
                              LocalDate month, String note) {}