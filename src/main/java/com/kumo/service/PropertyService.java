package com.kumo.service;

import com.kumo.domain.Property;
import com.kumo.domain.User;
import com.kumo.dto.PropertyRequest;
import com.kumo.dto.PropertyResponse;
import com.kumo.exception.ResourceNotFoundException;
import com.kumo.repository.PropertyRepository;
import com.kumo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

    @Transactional
    public PropertyResponse createProperty(String tenantEmail, PropertyRequest request) {
        User tenant = userRepository.findByEmail(tenantEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        Property property = Property.builder()
                .tenant(tenant)
                .address(request.getAddress().trim())
                .propertyType(request.getPropertyType())
                .landlordName(request.getLandlordName() != null ? request.getLandlordName().trim() : null)
                .landlordContact(request.getLandlordContact() != null ? request.getLandlordContact().trim() : null)
                .monthlyRent(request.getMonthlyRent())
                .tenancyStartDate(request.getTenancyStartDate())
                .active(true)
                .build();

        property = propertyRepository.save(property);
        return mapToResponse(property);
    }

    public List<PropertyResponse> getMyProperties(String tenantEmail) {
        User tenant = userRepository.findByEmail(tenantEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        return propertyRepository.findByTenantId(tenant.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public PropertyResponse mapToResponse(Property property) {
        return PropertyResponse.builder()
                .id(property.getId())
                .address(property.getAddress())
                .propertyType(property.getPropertyType())
                .landlordName(property.getLandlordName())
                .landlordContact(property.getLandlordContact())
                .monthlyRent(property.getMonthlyRent())
                .tenancyStartDate(property.getTenancyStartDate())
                .active(property.isActive())
                .createdAt(property.getCreatedAt())
                .build();
    }
}
