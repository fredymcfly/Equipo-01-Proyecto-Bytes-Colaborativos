package com.fleetcontrol.msauth.models.dto.response;

import com.fleetcontrol.msauth.models.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/** Response of {@code POST /api/auth/login}. */
public record LoginResponse(
    @Schema(example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...") String token,
    @Schema(example = "Bearer") String tokenType,
    @Schema(description = "Token lifetime in seconds", example = "3600") long expiresIn,
    @Schema(example = "550e8400-e29b-41d4-a716-446655440000") UUID userId,
    @Schema(example = "MANAGER") UserRole role) {}
