package com.kumo.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BankLinkStartResponse {
    private String sessionId;
    private String connectUrl;
    private String sessionToken;
    private long expiresInSeconds;
}
