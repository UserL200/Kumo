package com.kumo.repository;

import com.kumo.domain.TenantInvite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantInviteRepository extends JpaRepository<TenantInvite, Long> {
    Optional<TenantInvite> findByToken(String token);
}