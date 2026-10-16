package com.fleetcontrol.msdrivers.models.dto.response;

import com.fleetcontrol.msdrivers.models.enums.DriverStatus;
import com.fleetcontrol.msdrivers.models.enums.LicenseCategory;
import java.time.LocalDate;
import java.util.UUID;

/** Reduced representation of a driver, used in the paginated list. */
public record DriverSummaryResponse(
    UUID id,
    String fullName,
    String email,
    String phone,
    String licenseNumber,
    LicenseCategory licenseCategory,
    LocalDate licenseExpiresAt,
    DriverStatus status) {}
