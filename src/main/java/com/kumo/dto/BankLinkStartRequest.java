package com.kumo.dto;

import lombok.Data;

@Data
public class BankLinkStartRequest {
    private String redirectUrl;
    private String preferredInstitution; // e.g. "capitec", "fnb", "standard_bank"
}
