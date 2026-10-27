package com.kumo.service.provider;

import com.kumo.domain.User;
import com.kumo.dto.BankLinkStartRequest;
import com.kumo.dto.BankLinkStartResponse;
import com.kumo.dto.WebhookPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class MockBankVerificationProvider implements BankVerificationProvider {

    @Override
    public BankLinkStartResponse initiateLinkSession(User tenant, BankLinkStartRequest request) {
        String sessionId = "sess_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String sessionToken = "tok_connect_" + UUID.randomUUID().toString().substring(0, 12);

        String institution = (request != null && request.getPreferredInstitution() != null)
                ? request.getPreferredInstitution() : "capitec";

        String connectUrl = "https://connect.kumo.co.za/widget?session=" + sessionId + "&bank=" + institution;

        log.info("Initiated bank link session for user {}: sessionId={}", tenant.getEmail(), sessionId);

        return BankLinkStartResponse.builder()
                .sessionId(sessionId)
                .sessionToken(sessionToken)
                .connectUrl(connectUrl)
                .expiresInSeconds(1800)
                .build();
    }

    @Override
    public boolean processWebhook(WebhookPayload payload) {
        if (payload == null || payload.getSessionId() == null) {
            log.warn("Received empty or invalid webhook payload");
            return false;
        }
        log.info("Processed webhook event {} for sessionId={}", payload.getEventType(), payload.getSessionId());
        return true;
    }
}
