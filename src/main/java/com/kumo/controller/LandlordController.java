package com.kumo.controller;

import com.kumo.dto.InviteRequest;
import com.kumo.dto.InviteResponse;
import com.kumo.dto.PropertyRequest;
import com.kumo.dto.PropertyResponse;
import com.kumo.dto.RentRecordRequest;
import com.kumo.dto.RentRecordResponse;
import com.kumo.service.LandlordService;
import com.kumo.service.RentRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/landlord")
@RequiredArgsConstructor
public class LandlordController {

    private final LandlordService landlordService;
    private final RentRecordService rentRecordService;

    @PostMapping("/properties")
    public ResponseEntity<PropertyResponse> createProperty(
            Authentication auth, @Valid @RequestBody PropertyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(landlordService.createProperty(auth.getName(), request));
    }

    @GetMapping("/properties")
    public List<PropertyResponse> myProperties(Authentication auth) {
        return landlordService.getMyProperties(auth.getName());
    }

    @PostMapping("/properties/{propertyId}/invites")
    public ResponseEntity<com.kumo.domain.InviteResponse> invite(
            Authentication auth, @PathVariable Long propertyId, @Valid @RequestBody InviteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(landlordService.inviteTenant(auth.getName(), propertyId, request.email()));
    }

    @GetMapping("/properties/{propertyId}/payments")
    public List<RentRecordResponse> payments(Authentication auth, @PathVariable Long propertyId) {
        return rentRecordService.getPropertyRecords(auth.getName(), propertyId);
    }

    @PostMapping("/properties/{propertyId}/payments")
    public ResponseEntity<RentRecordResponse> recordPayment(
            Authentication auth, @PathVariable Long propertyId, @Valid @RequestBody RentRecordRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(rentRecordService.landlordRecordPayment(auth.getName(), propertyId, request));
    }
}