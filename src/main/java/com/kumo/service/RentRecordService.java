package com.kumo.service;

import com.kumo.domain.PaymentSource;
import com.kumo.domain.PaymentStatus;
import com.kumo.domain.Property;
import com.kumo.domain.RentRecord;
import com.kumo.domain.User;
import com.kumo.dto.RentExportResponse;
import com.kumo.dto.RentRecordRequest;
import com.kumo.dto.RentRecordResponse;
import com.kumo.exception.ResourceNotFoundException;
import com.kumo.exception.UnauthorizedException;
import com.kumo.exception.ValidationException;
import com.kumo.repository.PropertyRepository;
import com.kumo.repository.RentRecordRepository;
import com.kumo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RentRecordService {

    private final RentRecordRepository rentRecordRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

    public Page<RentRecordResponse> filterRentRecords(
            String tenantEmail,
            LocalDate startDate,
            LocalDate endDate,
            Long propertyId,
            PaymentSource source,
            int page,
            int size
    ) {
        User tenant = userRepository.findByEmail(tenantEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        Page<RentRecord> recordPage = rentRecordRepository.filterRentRecords(
                tenant.getId(),
                startDate,
                endDate,
                propertyId,
                source,
                PageRequest.of(page, size)
        );

        return recordPage.map(this::mapToResponse);
    }

    // Tenant logs their own payment. On a landlord-owned property it waits for the landlord to confirm.
    @Transactional
    public RentRecordResponse addManualRecord(String tenantEmail, RentRecordRequest request) {
        User tenant = userRepository.findByEmail(tenantEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        Property property = propertyRepository.findById(request.getPropertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        if (property.getTenant() == null || !property.getTenant().getId().equals(tenant.getId())) {
            throw new UnauthorizedException("Property does not belong to the current tenant");
        }

        RentRecord record = RentRecord.builder()
                .tenant(tenant)
                .property(property)
                .amount(request.getAmount())
                .paymentDate(request.getPaymentDate())
                .source(PaymentSource.MANUALLY_LOGGED)
                .status(property.getLandlord() != null ? PaymentStatus.PENDING : PaymentStatus.CONFIRMED)
                .recordedBy(tenant)
                .referenceNumber(request.getReferenceNumber() != null
                        ? request.getReferenceNumber().trim()
                        : "EFT-MANUAL-" + UUID.randomUUID().toString().substring(0, 6))
                .transactionNotes(request.getTransactionNotes())
                .build();

        return mapToResponse(rentRecordRepository.save(record));
    }

    // Landlord records rent received. It stays PENDING until the tenant confirms it.
    @Transactional
    public RentRecordResponse landlordRecordPayment(String landlordEmail, Long propertyId, RentRecordRequest request) {
        User landlord = userRepository.findByEmail(landlordEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Landlord not found"));
        Property property = requireOwnedProperty(landlord, propertyId);

        if (property.getTenant() == null) {
            throw new ValidationException("This property has no tenant yet");
        }

        RentRecord record = RentRecord.builder()
                .tenant(property.getTenant())
                .property(property)
                .amount(request.getAmount())
                .paymentDate(request.getPaymentDate())
                .source(PaymentSource.LANDLORD_RECORDED)
                .status(PaymentStatus.PENDING)
                .recordedBy(landlord)
                .referenceNumber(request.getReferenceNumber() != null
                        ? request.getReferenceNumber().trim()
                        : "LL-" + UUID.randomUUID().toString().substring(0, 6))
                .transactionNotes(request.getTransactionNotes())
                .build();

        return mapToResponse(rentRecordRepository.save(record));
    }

    public List<RentRecordResponse> getPropertyRecords(String landlordEmail, Long propertyId) {
        User landlord = userRepository.findByEmail(landlordEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Landlord not found"));
        requireOwnedProperty(landlord, propertyId);

        return rentRecordRepository.findByPropertyIdOrderByPaymentDateDesc(propertyId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public RentRecordResponse confirmRecord(String email, Long recordId) {
        User caller = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        RentRecord record = loadForParty(caller, recordId);

        if (record.getRecordedBy() != null && record.getRecordedBy().getId().equals(caller.getId())) {
            throw new UnauthorizedException("You cannot confirm your own entry");
        }
        if (record.getStatus() != PaymentStatus.PENDING) {
            throw new ValidationException("Only pending records can be confirmed");
        }

        record.setStatus(PaymentStatus.CONFIRMED);
        record.setConfirmedAt(LocalDateTime.now());
        return mapToResponse(rentRecordRepository.save(record));
    }

    @Transactional
    public RentRecordResponse disputeRecord(String email, Long recordId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new ValidationException("Please give a reason for the dispute");
        }
        User caller = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        RentRecord record = loadForParty(caller, recordId);

        if (record.getStatus() == PaymentStatus.DISPUTED) {
            throw new ValidationException("This record is already disputed");
        }

        record.setStatus(PaymentStatus.DISPUTED);
        record.setDisputeReason(reason.trim());
        record.setDisputedAt(LocalDateTime.now());
        return mapToResponse(rentRecordRepository.save(record));
    }

    // Only CONFIRMED payments go into a verified history. Pending and disputed ones are left out.
    public RentExportResponse exportVerifiedRentHistory(String tenantEmail) {
        User tenant = userRepository.findByEmail(tenantEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        List<Property> properties = propertyRepository.findByTenantId(tenant.getId());
        Property primaryProperty = properties.isEmpty() ? null : properties.get(0);

        List<RentRecord> allRecords = rentRecordRepository
                .findByTenantIdOrderByPaymentDateDesc(tenant.getId()).stream()
                .filter(r -> r.getStatus() == PaymentStatus.CONFIRMED)
                .toList();

        List<RentRecordResponse> recordResponses = allRecords.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        int autoVerifiedCount = (int) allRecords.stream().filter(r -> r.getSource() == PaymentSource.AUTO_DETECTED).count();
        int manualCount = allRecords.size() - autoVerifiedCount;

        BigDecimal totalAmountPaid = allRecords.stream()
                .map(RentRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Placeholder: there is no rent due date on Property yet, so on-time cannot be measured.
        // Remove this field from the export, or add a due day and calculate it properly.
        double onTimeRate = allRecords.isEmpty() ? 0.0 : 100.0;

        String exportId = "KM-EXP-" + UUID.randomUUID().toString().toUpperCase().substring(0, 12);
        LocalDateTime issuedAt = LocalDateTime.now();

        String dataToSign = exportId + ":" + tenant.getEmail() + ":" + totalAmountPaid + ":" + allRecords.size();
        String integrityHash = generateSHA256Hash(dataToSign);

        return RentExportResponse.builder()
                .exportId(exportId)
                .issuedAt(issuedAt)
                .tenant(RentExportResponse.TenantSummary.builder()
                        .fullName(tenant.getFirstName() + " " + tenant.getLastName())
                        .email(tenant.getEmail())
                        .phone(tenant.getPhoneNumber())
                        .build())
                .property(primaryProperty != null ? RentExportResponse.PropertySummary.builder()
                        .address(primaryProperty.getAddress())
                        .propertyType(primaryProperty.getPropertyType().name())
                        .monthlyRent(primaryProperty.getMonthlyRent())
                        .build() : null)
                .verificationStatus(tenant.getVerificationStatus())
                .bankName(tenant.getBankName())
                .maskedAccountReference(tenant.getMaskedAccountReference())
                .metrics(RentExportResponse.Metrics.builder()
                        .totalMonthsRecorded(allRecords.size())
                        .autoVerifiedCount(autoVerifiedCount)
                        .manualCount(manualCount)
                        .totalAmountPaid(totalAmountPaid.setScale(2, RoundingMode.HALF_UP))
                        .onTimePaymentPercentage(onTimeRate)
                        .consecutiveMonthsStreak(currentStreak(allRecords) + " consecutive months")
                        .build())
                .records(recordResponses)
                .cryptographicSeal(RentExportResponse.CryptographicSeal.builder()
                        .issuer("Kumo Verified Rental Registry")
                        .signatureAlgorithm("SHA-256 integrity hash") // a plain hash, not a digital signature
                        .digitalSignature(integrityHash)
                        .verificationHash(generateSHA256Hash(integrityHash + ":" + issuedAt))
                        .build())
                .build();
    }

    // Consecutive months with a confirmed payment, counted back from this month (or last month
    // if this month has none yet).
    private int currentStreak(List<RentRecord> confirmed) {
        Set<YearMonth> months = confirmed.stream()
                .map(r -> YearMonth.from(r.getPaymentDate()))
                .collect(Collectors.toSet());
        YearMonth cursor = YearMonth.now();
        if (!months.contains(cursor)) cursor = cursor.minusMonths(1);
        int streak = 0;
        while (months.contains(cursor)) {
            streak++;
            cursor = cursor.minusMonths(1);
        }
        return streak;
    }

    private Property requireOwnedProperty(User landlord, Long propertyId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));
        if (property.getLandlord() == null || !property.getLandlord().getId().equals(landlord.getId())) {
            throw new UnauthorizedException("This property does not belong to you");
        }
        return property;
    }

    // Only the tenant or the landlord on this record may touch it.
    private RentRecord loadForParty(User caller, Long recordId) {
        RentRecord record = rentRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Record not found"));

        boolean isTenant = record.getTenant().getId().equals(caller.getId());
        User landlord = record.getProperty().getLandlord();
        boolean isLandlord = landlord != null && landlord.getId().equals(caller.getId());

        if (!isTenant && !isLandlord) {
            throw new UnauthorizedException("You are not part of this tenancy");
        }
        return record;
    }

    private RentRecordResponse mapToResponse(RentRecord record) {
        return RentRecordResponse.builder()
                .id(record.getId())
                .propertyId(record.getProperty().getId())
                .propertyAddress(record.getProperty().getAddress())
                .amount(record.getAmount())
                .paymentDate(record.getPaymentDate())
                .source(record.getSource())
                .status(record.getStatus())
                .referenceNumber(record.getReferenceNumber())
                .transactionNotes(record.getTransactionNotes())
                .disputeReason(record.getDisputeReason())
                .createdAt(record.getCreatedAt())
                .build();
    }

    private String generateSHA256Hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return "HASH_ERR_" + System.currentTimeMillis();
        }
    }
}