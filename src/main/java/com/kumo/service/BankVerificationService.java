package com.kumo.service;

import com.kumo.domain.*;
import com.kumo.dto.*;
import com.kumo.exception.ResourceNotFoundException;
import com.kumo.repository.BankLinkSessionRepository;
import com.kumo.repository.PropertyRepository;
import com.kumo.repository.RentRecordRepository;
import com.kumo.repository.UserRepository;
import com.kumo.service.provider.BankVerificationProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankVerificationService {

    private final UserRepository userRepository;
    private final BankLinkSessionRepository sessionRepository;
    private final PropertyRepository propertyRepository;
    private final RentRecordRepository rentRecordRepository;
    private final BankVerificationProvider verificationProvider;

    @Transactional
    public BankLinkStartResponse startBankLink(String tenantEmail, BankLinkStartRequest request) {
        User tenant = userRepository.findByEmail(tenantEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        BankLinkStartResponse response = verificationProvider.initiateLinkSession(tenant, request);

        BankLinkSession session = BankLinkSession.builder()
                .sessionId(response.getSessionId())
                .tenant(tenant)
                .status(VerificationStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusSeconds(response.getExpiresInSeconds()))
                .build();

        sessionRepository.save(session);

        tenant.setVerificationStatus(VerificationStatus.PENDING);
        userRepository.save(tenant);

        return response;
    }

    @Transactional
    public boolean handleWebhook(WebhookPayload payload) {
        log.info("Received webhook event for sessionId={}", payload.getSessionId());

        boolean providerProcessed = verificationProvider.processWebhook(payload);
        if (!providerProcessed) {
            return false;
        }

        BankLinkSession session = sessionRepository.findBySessionId(payload.getSessionId())
                .orElse(null);

        if (session == null) {
            log.warn("Session ID {} not found in session repository", payload.getSessionId());
            return false;
        }

        User tenant = session.getTenant();
        VerificationStatus newStatus = payload.getStatus() != null ? payload.getStatus() : VerificationStatus.VERIFIED;

        session.setStatus(newStatus);
        sessionRepository.save(session);

        tenant.setVerificationStatus(newStatus);
        if (payload.getBankName() != null) {
            tenant.setBankName(payload.getBankName());
        }
        if (payload.getAccountLast4() != null) {
            tenant.setMaskedAccountReference("•••• " + payload.getAccountLast4());
        }
        if (payload.getProviderToken() != null) {
            // Simulated encrypted storage at rest
            tenant.setEncryptedProviderToken("ENC(" + payload.getProviderToken() + ")");
        }
        tenant.setBankLinkedAt(LocalDateTime.now());
        userRepository.save(tenant);

        // Auto-detect rent records if payload includes detected transactions
        if (payload.getDetectedRentTransactions() != null && !payload.getDetectedRentTransactions().isEmpty()) {
            List<Property> activeProperties = propertyRepository.findByTenantIdAndActiveTrue(tenant.getId());
            Property primaryProperty = activeProperties.isEmpty() ? null : activeProperties.get(0);

            if (primaryProperty != null) {
                for (WebhookPayload.DetectedTransaction dt : payload.getDetectedRentTransactions()) {
                    RentRecord record = RentRecord.builder()
                            .tenant(tenant)
                            .property(primaryProperty)
                            .amount(BigDecimal.valueOf(dt.getAmount()))
                            .paymentDate(LocalDate.parse(dt.getDate()))
                            .source(PaymentSource.AUTO_DETECTED)
                            .status(PaymentStatus.CONFIRMED)
                            .referenceNumber(dt.getReference())
                            .transactionNotes("Auto-verified from bank feed (" + tenant.getBankName() + ")")
                            .externalTransactionId(dt.getExternalId())
                            .build();

                    rentRecordRepository.save(record);
                }
            }
        }

        return true;
    }

    public VerificationStatusResponse getTenantVerificationStatus(String tenantEmail) {
        User tenant = userRepository.findByEmail(tenantEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        List<RentRecord> records = rentRecordRepository.findByTenantIdOrderByPaymentDateDesc(tenant.getId());
        long autoVerifiedCount = records.stream()
                .filter(r -> r.getSource() == PaymentSource.AUTO_DETECTED)
                .count();

        return VerificationStatusResponse.builder()
                .status(tenant.getVerificationStatus())
                .maskedAccountReference(tenant.getMaskedAccountReference())
                .bankName(tenant.getBankName())
                .bankLinkedAt(tenant.getBankLinkedAt())
                .verifiedTransactionsCount((int) autoVerifiedCount)
                .build();
    }
}
