package com.kumo.dto;

import java.time.LocalDateTime;

public record InviteResponse(String token, String email, LocalDateTime expiresAt) {}