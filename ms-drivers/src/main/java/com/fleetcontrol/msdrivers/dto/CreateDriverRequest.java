
package com.fleetcontrol.msdrivers.dto;

import com.fleetcontrol.msdrivers.model.LicenseCategory;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateDriverRequest(

        @NotBlank
        String fullName,

        @NotBlank
        @Email
        String email,

        String phone,

        @NotBlank
        String licenseNumber,

        @NotNull
        LicenseCategory licenseCategory,

        @NotNull
        @Future
        LocalDate licenseExpiresAt

) {
}
