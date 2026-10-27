package com.kumo.dto;

import com.kumo.domain.PaymentSource;
import com.kumo.domain.PaymentStatus;
import com.kumo.domain.RentRecord.RentRecordBuilder;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builderpublic RentRecordBuilder disputeReason(Object disputeReason) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'disputeReason'");
    }
public class RentRecordResponse {
    private Long id;
    private Long propertyId;
    private String propertyAddress;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private PaymentSource source;
    private PaymentStatus status;
    private String referenceNumber;
    private String transactionNotes;
    private LocalDateTime createdAt;
    public static RentRecordBuilder builder() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'builder'");
    }
}
