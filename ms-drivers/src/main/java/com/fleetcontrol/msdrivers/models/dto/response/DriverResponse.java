package com.fleetcontrol.msdrivers.models.dto.response;

import com.fleetcontrol.msdrivers.models.enums.DriverStatus;
import com.fleetcontrol.msdrivers.models.enums.LicenseCategory;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Detailed representation of a driver. */
public record DriverResponse(
    UUID id,
    String fullName,
    String email,
    String phone,
    String licenseNumber,
    LicenseCategory licenseCategory,
    LocalDate licenseExpiresAt,
    DriverStatus status,
    Instant createdAt,
    Instant updatedAt) {}
