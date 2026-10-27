package com.kumo.service.provider;

import com.kumo.domain.User;
import com.kumo.dto.BankLinkStartRequest;
import com.kumo.dto.BankLinkStartResponse;
import com.kumo.dto.WebhookPayload;

public interface BankVerificationProvider {

    /**
     * Initiates a bank account linking session for a tenant.
     *
     * @param tenant The tenant initiating the bank link
     * @param request Additional parameters such as redirect URI or preferred institution
     * @return BankLinkStartResponse containing session tokens and connect URL
     */
    BankLinkStartResponse initiateLinkSession(User tenant, BankLinkStartRequest request);

    /**
     * Validates and processes a incoming verification webhook payload from the provider.
     *
     * @param payload The webhook payload received
     * @return True if webhook signature/payload was valid and processed successfully
     */
    boolean processWebhook(WebhookPayload payload);
}
