package com.kumo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaitlistJoinResponse {
    // Deliberately the same message whether this email was already on
    // the list or brand new — never reveal which, so the endpoint
    // can't be used to check if a given email exists in our system.
    private String message;
}
