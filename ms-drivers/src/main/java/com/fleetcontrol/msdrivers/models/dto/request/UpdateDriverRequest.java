package com.fleetcontrol.msdrivers.models.dto.request;

import com.fleetcontrol.msdrivers.constants.BusinessConstants;
import com.fleetcontrol.msdrivers.models.enums.DriverStatus;
import com.fleetcontrol.msdrivers.models.enums.LicenseCategory;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Payload used to update every data of a driver, including the status. The expiration date is not
 * required to be in the future, so a driver with an expired license can still be edited.
 */
public record UpdateDriverRequest(
    @NotBlank @Size(max = BusinessConstants.FieldLimits.FULL_NAME_MAX) String fullName,
    @NotBlank @Email @Size(max = BusinessConstants.FieldLimits.EMAIL_MAX) String email,
    @Size(max = BusinessConstants.FieldLimits.PHONE_MAX) String phone,
    @NotBlank @Size(max = BusinessConstants.FieldLimits.LICENSE_NUMBER_MAX) String licenseNumber,
    @NotNull LicenseCategory licenseCategory,
    @NotNull LocalDate licenseExpiresAt,
    @NotNull DriverStatus status) {}
