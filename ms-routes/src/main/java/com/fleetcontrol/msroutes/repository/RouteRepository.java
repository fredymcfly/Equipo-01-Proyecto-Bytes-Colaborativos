package com.fleetcontrol.msroutes.repository;

import com.fleetcontrol.msroutes.entities.Route;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence access for routes. */
public interface RouteRepository
    extends JpaRepository<Route, UUID>, JpaSpecificationExecutor<Route> {

  /**
   * Checks if the vehicle has a planned or in-progress route overlapping the given interval.
   * Overlap: two intervals overlap if existing_start < new_end and existing_end > new_start. Using
   * strict and > comparisons, a route that ends exactly when another begins is not considered an
   * overlap. The end time is calculated as planned_start + estimated_duration_min, without using a
   * derived column.
   *
   * @param vehicleId vehicle to check
   * @param start start of the new interval
   * @param end end of the nuw interval
   */
  @Query(
      value =
          """
            SELECT EXISTS (
                SELECT 1 FROM routes r
                WHERE r.vehicle_id = :vehicleId
                    AND r.status IN ('PLANNED', 'IN_PROGRESS')
                    AND r.planned_start < :end
                    AND r.planned_start + make_interval(mins => r.estimated_duration_min) > :start
            )
            """,
      nativeQuery = true)
  boolean existsVehicleOverlap(
      @Param("vehicleId") UUID vehicleId, @Param("start") Instant start, @Param("end") Instant end);

  /**
   * Checks if the driver has a planned or in-progress route overlapping the given interval.
   *
   * @param driverId driver to check
   * @param start start of the new interval
   * @param end end of the new interval
   */
  @Query(
      value =
          """
            SELECT EXISTS (
                SELECT 1 FROM routes r
                WHERE r.driver_id = :driverId
                    AND r.status IN ('PLANNED', 'IN_PROGRESS')
                    AND r.planned_start < :end
                    AND r.planned_start + make_interval(mins => r.estimated_duration_min) > :start
            )
            """,
      nativeQuery = true)
  boolean existsDriverOverlap(
      @Param("driverId") UUID driverId, @Param("start") Instant start, @Param("end") Instant end);
}
