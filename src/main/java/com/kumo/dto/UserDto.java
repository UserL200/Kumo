package com.kumo.dto;

import com.kumo.domain.Role;
import com.kumo.domain.VerificationStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class UserDto {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private Role role;
    private VerificationStatus verificationStatus;
    private String maskedAccountReference;
    private String bankName;
    private LocalDateTime bankLinkedAt;
    private LocalDateTime createdAt;
}
