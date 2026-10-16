package com.fleetcontrol.msvehicles.repository;

import com.fleetcontrol.msvehicles.models.entities.Vehicle;
import com.fleetcontrol.msvehicles.models.enums.VehicleStatus;
import com.fleetcontrol.msvehicles.models.enums.VehicleType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

/** Data access for fleet vehicles. */
public interface VehicleRepository
    extends JpaRepository<Vehicle, UUID>, JpaSpecificationExecutor<Vehicle> {

  boolean existsByPlate(String plate);

  /** Number of vehicles of every status that has at least one vehicle. */
  @Query("select v.status as vehicleStatus, count(v) as total from Vehicle v group by v.status")
  List<StatusCount> countGroupedByStatus();

  /** Number of vehicles of every type that has at least one vehicle. */
  @Query("select v.type as vehicleType, count(v) as total from Vehicle v group by v.type")
  List<TypeCount> countGroupedByType();

  /** Vehicles counted for one status. */
  interface StatusCount {
    VehicleStatus getVehicleStatus();

    long getTotal();
  }

  /** Vehicles counted for one type. */
  interface TypeCount {
    VehicleType getVehicleType();

    long getTotal();
  }
}
