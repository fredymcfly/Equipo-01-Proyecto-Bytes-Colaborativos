
package com.fleetcontrol.msdrivers.repository;

import com.fleetcontrol.msdrivers.model.Driver;
import com.fleetcontrol.msdrivers.model.DriverStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DriverRepository extends JpaRepository<Driver, UUID> {

    boolean existsByLicenseNumber(String licenseNumber);

    Page<Driver> findByStatus(DriverStatus status, Pageable pageable);
}
