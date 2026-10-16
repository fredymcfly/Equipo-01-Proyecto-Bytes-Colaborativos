package com.fleetcontrol.msdrivers.repository;

import com.fleetcontrol.msdrivers.models.entities.Driver;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Data access for fleet drivers. */
public interface DriverRepository
    extends JpaRepository<Driver, UUID>, JpaSpecificationExecutor<Driver> {

  boolean existsByLicenseNumber(String licenseNumber);

  boolean existsByLicenseNumberAndIdNot(String licenseNumber, UUID id);
}
