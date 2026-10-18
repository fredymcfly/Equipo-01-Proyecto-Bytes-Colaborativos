package com.fleetcontrol.msvehicles.repository;

import com.fleetcontrol.msvehicles.models.entities.Vehicle;
import com.fleetcontrol.msvehicles.models.enums.VehicleStatus;
import com.fleetcontrol.msvehicles.models.enums.VehicleType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

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

  /**
   * Replaces the id of a vehicle. Only the demo seeder uses it, to give the demo vehicles the same
   * ids in every run. The write is flushed first and the persistence context is cleared after it.
   */
  @Transactional
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(value = "UPDATE vehicles SET id = :newId WHERE id = :oldId", nativeQuery = true)
  int reassignId(@Param("oldId") UUID oldId, @Param("newId") UUID newId);
}
