package com.fleetcontrol.msroutes.client.dto;

import com.fleetcontrol.msroutes.client.enums.DriverStatus;
import com.fleetcontrol.msroutes.client.enums.LicenseCategory;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Driver data returned by the drivers service.
 *
 * @param id driver identifier
 * @param licenseCategory driver's license category
 * @param licenseExpiresAt license expiration date
 * @param status driver status
 */
public record DriverResponse(
    UUID id, LicenseCategory licenseCategory, LocalDate licenseExpiresAt, DriverStatus status) {}
