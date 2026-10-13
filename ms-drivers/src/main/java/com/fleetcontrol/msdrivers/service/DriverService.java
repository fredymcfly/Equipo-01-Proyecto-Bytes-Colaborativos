
package com.fleetcontrol.msdrivers.service;

import com.fleetcontrol.msdrivers.model.Driver;
import com.fleetcontrol.msdrivers.model.DriverStatus;
import com.fleetcontrol.msdrivers.repository.DriverRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import com.fleetcontrol.msdrivers.exception.DriverAlreadyExistsException;
import com.fleetcontrol.msdrivers.exception.DriverValidationException;
import java.util.UUID;
import com.fleetcontrol.msdrivers.exception.DriverNotFoundException;
@Service
public class DriverService {

    private final DriverRepository driverRepository;

    public DriverService(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    public Driver createDriver(Driver driver) {

        if (driverRepository.existsByLicenseNumber(driver.getLicenseNumber())) {
            throw new DriverAlreadyExistsException();
        }

        if (driver.getLicenseExpiresAt() == null ||
                !driver.getLicenseExpiresAt().isAfter(LocalDate.now())) {
            throw new DriverValidationException();
        }

        driver.setId(null);
        driver.setStatus(DriverStatus.ACTIVE);

        return driverRepository.save(driver);
    }


    public Driver getDriverById(UUID id) {

        return driverRepository.findById(id)
                .orElseThrow(DriverNotFoundException::new);
    }


}
