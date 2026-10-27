package com.kumo.controller;

import com.kumo.dto.DisputeRequest;
import com.kumo.dto.PropertyResponse;
import com.kumo.dto.RentRecordResponse;
import com.kumo.service.LandlordService;
import com.kumo.service.RentRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TenancyActionController {

    private final LandlordService landlordService;
    private final RentRecordService rentRecordService;

    @PostMapping("/invites/{token}/accept")
    public PropertyResponse accept(Authentication auth, @PathVariable String token) {
        return landlordService.acceptInvite(auth.getName(), token);
    }

    @PostMapping("/payments/{id}/confirm")
    public RentRecordResponse confirm(Authentication auth, @PathVariable Long id) {
        return rentRecordService.confirmRecord(auth.getName(), id);
    }

    @PostMapping("/payments/{id}/dispute")
    public RentRecordResponse dispute(
            Authentication auth, @PathVariable Long id, @Valid @RequestBody DisputeRequest request) {
        return rentRecordService.disputeRecord(auth.getName(), id, request.reason());
    }
}