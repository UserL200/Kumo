package com.kumo.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kumo.domain.BudgetExpense;



public interface BudgetExpenseRepository extends JpaRepository<BudgetExpense, Long> {
    List<BudgetExpense> findByUserIdAndMonthOrderByCreatedAtDesc(Long userId, LocalDate month);
    Optional<BudgetExpense> findByIdAndUserId(Long id, Long userId);
} 
    

