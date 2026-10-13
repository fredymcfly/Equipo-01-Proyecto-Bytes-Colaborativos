
package com.fleetcontrol.msdrivers.controller;

import com.fleetcontrol.msdrivers.dto.CreateDriverRequest;
import com.fleetcontrol.msdrivers.model.Driver;
import com.fleetcontrol.msdrivers.service.DriverService;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @PostMapping
    public ResponseEntity<Driver> createDriver(
            @Valid @RequestBody CreateDriverRequest request) {

        Driver driver = new Driver();

        driver.setFullName(request.fullName());
        driver.setEmail(request.email());
        driver.setPhone(request.phone());
        driver.setLicenseNumber(request.licenseNumber());
        driver.setLicenseCategory(request.licenseCategory());
        driver.setLicenseExpiresAt(request.licenseExpiresAt());

        Driver createdDriver = driverService.createDriver(driver);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdDriver);
    }


    @GetMapping("/{driverId}")
    public ResponseEntity<Driver> getDriverById(
            @PathVariable UUID driverId) {

        Driver driver = driverService.getDriverById(driverId);

        return ResponseEntity.ok(driver);
    }

}
