package com.kumo.dto;

import com.kumo.domain.VerificationStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class VerificationStatusResponse {
    private VerificationStatus status;
    private String maskedAccountReference;
    private String bankName;
    private LocalDateTime bankLinkedAt;
    private int verifiedTransactionsCount;
}
