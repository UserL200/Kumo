package com.kumo.dto;

import jakarta.validation.constraints.NotBlank;

public record DisputeRequest(@NotBlank String reason) {}