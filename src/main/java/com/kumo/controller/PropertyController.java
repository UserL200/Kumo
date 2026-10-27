package com.kumo.controller;

import com.kumo.dto.PropertyRequest;
import com.kumo.dto.PropertyResponse;
import com.kumo.security.SecurityUtils;
import com.kumo.service.PropertyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/properties")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService propertyService;

    @GetMapping("/mine")
    public ResponseEntity<List<PropertyResponse>> getMyProperties() {
        String tenantEmail = SecurityUtils.getCurrentUserEmail();
        List<PropertyResponse> properties = propertyService.getMyProperties(tenantEmail);
        return ResponseEntity.ok(properties);
    }

    @PostMapping
    public ResponseEntity<PropertyResponse> createProperty(@Valid @RequestBody PropertyRequest request) {
        String tenantEmail = SecurityUtils.getCurrentUserEmail();
        PropertyResponse property = propertyService.createProperty(tenantEmail, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(property);
    }
}
