package com.fleetcontrol.msdrivers.repository;

import com.fleetcontrol.msdrivers.models.entities.Driver;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/** Data access for fleet drivers. */
public interface DriverRepository
    extends JpaRepository<Driver, UUID>, JpaSpecificationExecutor<Driver> {

  boolean existsByLicenseNumber(String licenseNumber);

  boolean existsByLicenseNumberAndIdNot(String licenseNumber, UUID id);

  /**
   * Replaces the id of a driver. Only the demo seeder uses it, to give the demo drivers the same
   * ids in every run. The write is flushed first and the persistence context is cleared after it.
   */
  @Transactional
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(value = "UPDATE drivers SET id = :newId WHERE id = :oldId", nativeQuery = true)
  int reassignId(@Param("oldId") UUID oldId, @Param("newId") UUID newId);
}
