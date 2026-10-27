package com.kumo.dto;

import com.kumo.domain.VerificationStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class RentExportResponse {
    private String exportId;
    private LocalDateTime issuedAt;
    private TenantSummary tenant;
    private PropertySummary property;
    private VerificationStatus verificationStatus;
    private String bankName;
    private String maskedAccountReference;
    private Metrics metrics;
    private List<RentRecordResponse> records;
    private CryptographicSeal cryptographicSeal;

    @Data
    @Builder
    public static class TenantSummary {
        private String fullName;
        private String email;
        private String phone;
    }

    @Data
    @Builder
    public static class PropertySummary {
        private String address;
        private String propertyType;
        private BigDecimal monthlyRent;
    }

    @Data
    @Builder
    public static class Metrics {
        private int totalMonthsRecorded;
        private int autoVerifiedCount;
        private int manualCount;
        private BigDecimal totalAmountPaid;
        private double onTimePaymentPercentage;
        private String consecutiveMonthsStreak;
    }

    @Data
    @Builder
    public static class CryptographicSeal {
        private String issuer;
        private String signatureAlgorithm;
        private String digitalSignature;
        private String verificationHash;
    }
}
