package com.kumo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kumo.domain.BudgetProfile;

public interface BudgetProfileRepository extends JpaRepository<BudgetProfile, Long> {
    Optional<BudgetProfile> findByUserId(Long userId);
}