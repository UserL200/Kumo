package com.kumo.repository;

import com.kumo.domain.BankLinkSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BankLinkSessionRepository extends JpaRepository<BankLinkSession, Long> {
    Optional<BankLinkSession> findBySessionId(String sessionId);
}
