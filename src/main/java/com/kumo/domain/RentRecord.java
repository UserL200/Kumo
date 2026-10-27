package com.kumo.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "rent_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private User tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    private String referenceNumber;

    @Column(length = 1000)
    private String transactionNotes;

    // External provider transaction ID if auto-detected
    private String externalTransactionId;

    private LocalDateTime createdAt = LocalDateTime.now();

    public void setConfirmedAt(LocalDateTime now) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setConfirmedAt'");
    }

    public void setDisputeReason(String trim) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setDisputeReason'");
    }

    public void setDisputedAt(LocalDateTime now) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setDisputedAt'");
    }

    public Object getDisputeReason() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getDisputeReason'");
    }
}
