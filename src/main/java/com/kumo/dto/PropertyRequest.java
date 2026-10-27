package com.kumo.dto;

import com.kumo.domain.PropertyType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PropertyRequest {

    @NotBlank(message = "Address is required")
    private String address;

    @NotNull(message = "Property type is required")
    private PropertyType propertyType;

    // Optional
    private String landlordName;

    // Optional
    private String landlordContact;

    @NotNull(message = "Monthly rent is required")
    @DecimalMin(value = "0.01", message = "Rent amount must be greater than zero")
    private BigDecimal monthlyRent;

    private LocalDate tenancyStartDate;
}
