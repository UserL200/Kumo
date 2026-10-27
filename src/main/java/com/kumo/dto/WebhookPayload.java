package com.kumo.dto;

import com.kumo.domain.VerificationStatus;
import lombok.Data;
import java.util.List;

@Data
public class WebhookPayload {
    private String eventType; // e.g. "account.linked", "verification.completed", "verification.failed"
    private String sessionId;
    private String providerReference;
    private VerificationStatus status;
    private String bankName;
    private String accountLast4;
    private String providerToken;
    private List<DetectedTransaction> detectedRentTransactions;

    @Data
    public static class DetectedTransaction {
        private String externalId;
        private Double amount;
        private String date; // YYYY-MM-DD
        private String reference;
        private String counterparty;
    }
}
