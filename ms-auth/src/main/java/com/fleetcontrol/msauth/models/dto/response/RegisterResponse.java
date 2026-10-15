package com.fleetcontrol.msauth.models.dto.response;

import com.fleetcontrol.msauth.models.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/** Response of {@code POST /api/auth/register}. It never includes the password. */
public record RegisterResponse(
    @Schema(example = "550e8400-e29b-41d4-a716-446655440000") UUID id,
    @Schema(example = "carlos_ruiz") String username,
    @Schema(example = "carlos@fleetcontrol.com") String email,
    @Schema(example = "MANAGER") UserRole role,
    @Schema(example = "2026-10-05T10:30:00Z") Instant createdAt) {}
