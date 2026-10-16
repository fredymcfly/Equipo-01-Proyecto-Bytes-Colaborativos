package com.fleetcontrol.msdrivers.models.dto.request;

import com.fleetcontrol.msdrivers.constants.BusinessConstants;
import com.fleetcontrol.msdrivers.models.enums.LicenseCategory;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Payload used to register a new driver. */
public record CreateDriverRequest(
    @NotBlank @Size(max = BusinessConstants.FieldLimits.FULL_NAME_MAX) String fullName,
    @NotBlank @Email @Size(max = BusinessConstants.FieldLimits.EMAIL_MAX) String email,
    @Size(max = BusinessConstants.FieldLimits.PHONE_MAX) String phone,
    @NotBlank @Size(max = BusinessConstants.FieldLimits.LICENSE_NUMBER_MAX) String licenseNumber,
    @NotNull LicenseCategory licenseCategory,
    @NotNull @Future LocalDate licenseExpiresAt) {}
