package com.kumo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WaitlistJoinRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    private String email;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    // Honeypot field. Real users never see or fill this (hidden on the
    // frontend); bots that blindly fill every field they find will.
    // Named to look like a plausible real field to a scraping bot,
    // rather than something obviously named "honeypot".
    private String website;
}
