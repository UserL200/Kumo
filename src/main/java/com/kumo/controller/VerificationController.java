package com.kumo.controller;

import com.kumo.dto.BankLinkStartRequest;
import com.kumo.dto.BankLinkStartResponse;
import com.kumo.dto.VerificationStatusResponse;
import com.kumo.dto.WebhookPayload;
import com.kumo.security.SecurityUtils;
import com.kumo.service.BankVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/verification")
@RequiredArgsConstructor
public class VerificationController {

    private final BankVerificationService verificationService;

    @PostMapping("/start")
    public ResponseEntity<BankLinkStartResponse> startVerification(@RequestBody(required = false) BankLinkStartRequest request) {
        String tenantEmail = SecurityUtils.getCurrentUserEmail();
        BankLinkStartResponse response = verificationService.startBankLink(tenantEmail, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> handleWebhook(@RequestBody WebhookPayload payload) {
        boolean processed = verificationService.handleWebhook(payload);
        if (processed) {
            return ResponseEntity.ok(Map.of("status", "success", "message", "Webhook processed successfully"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Failed to process webhook"));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<VerificationStatusResponse> getStatus() {
        String tenantEmail = SecurityUtils.getCurrentUserEmail();
        VerificationStatusResponse status = verificationService.getTenantVerificationStatus(tenantEmail);
        return ResponseEntity.ok(status);
    }
}
