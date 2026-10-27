package com.kumo.repository;

import com.kumo.domain.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PropertyRepository extends JpaRepository<Property, Long> {
    List<Property> findByTenantId(Long tenantId);
    List<Property> findByTenantIdAndActiveTrue(Long tenantId);
    List<Property> findByLandlordId(Long landlordId);
}
