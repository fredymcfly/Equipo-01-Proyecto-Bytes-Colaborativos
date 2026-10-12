package com.fleetcontrol.msvehicles.repository;

import com.fleetcontrol.msvehicles.model.Vehicle;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Data access for fleet vehicles. */
public interface VehicleRepository
    extends JpaRepository<Vehicle, UUID>, JpaSpecificationExecutor<Vehicle> {

  boolean existsByPlate(String plate);
}
