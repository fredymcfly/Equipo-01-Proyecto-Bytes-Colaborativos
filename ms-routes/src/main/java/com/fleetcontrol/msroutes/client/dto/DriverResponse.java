package com.fleetcontrol.msroutes.client.dto;

import com.fleetcontrol.msroutes.client.enums.DriverStatus;
import com.fleetcontrol.msroutes.client.enums.LicenseCategory;

import java.time.LocalDate;
import java.util.UUID;

public record DriverResponse(
  UUID id,
  LicenseCategory licenseCategory,
  LocalDate licenseExpiresAt,
  DriverStatus status
) {
}
