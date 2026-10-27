package com.kumo.dto;

import com.kumo.domain.PropertyType;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class PropertyResponse {
    private Long id;
    private String address;
    private PropertyType propertyType;
    private String landlordName;
    private String landlordContact;
    private BigDecimal monthlyRent;
    private LocalDate tenancyStartDate;
    private boolean active;
    private LocalDateTime createdAt;
}
