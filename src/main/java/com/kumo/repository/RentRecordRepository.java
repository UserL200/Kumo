package com.kumo.repository;

import com.kumo.domain.PaymentSource;
import com.kumo.domain.RentRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface RentRecordRepository extends JpaRepository<RentRecord, Long> {

    List<RentRecord> findByTenantIdOrderByPaymentDateDesc(Long tenantId);
    List<RentRecord> findByPropertyIdOrderByPaymentDateDesc(Long propertyId);


    @Query("SELECT r FROM RentRecord r WHERE r.tenant.id = :tenantId " +
           "AND (:startDate IS NULL OR r.paymentDate >= :startDate) " +
           "AND (:endDate IS NULL OR r.paymentDate <= :endDate) " +
           "AND (:propertyId IS NULL OR r.property.id = :propertyId) " +
           "AND (:source IS NULL OR r.source = :source) " +
           "ORDER BY r.paymentDate DESC")
    Page<RentRecord> filterRentRecords(
            @Param("tenantId") Long tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("propertyId") Long propertyId,
            @Param("source") PaymentSource source,
            Pageable pageable
    );
}
