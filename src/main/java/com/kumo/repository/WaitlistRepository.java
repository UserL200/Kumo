package com.kumo.repository;

import com.kumo.domain.WaitlistEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WaitlistRepository extends JpaRepository<WaitlistEntry, Long> {
    Optional<WaitlistEntry> findByEmail(String email);
    boolean existsByEmail(String email);
}
