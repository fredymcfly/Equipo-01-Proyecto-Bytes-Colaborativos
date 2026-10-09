package com.fleetcontrol.msroutes.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fleetcontrol.msroutes.entities.Route;
import com.fleetcontrol.msroutes.enums.RouteStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Route as exposed by the API; fields without a value are omitted.
 *
 * @param id route identifier
 * @param vehicleId assigned vehicle identifier
 * @param driverId assigned driver identifier
 * @param origin route origin
 * @param destination route destination
 * @param plannedStart planned start timestamp
 * @param estimatedDurationMin estimated duration in minutes
 * @param plannedDistanceKm planned distance in kilometres
 * @param status current route status
 * @param startedAt actual start timestamp
 * @param endedAt actual end timestamp
 * @param startOdometerKm odometer reading at start
 * @param endOdometerKm odometer reading at end
 * @param actualDistanceKm actual distance travelled
 * @param notes additional route notes
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RouteResponse(
    UUID id,
    UUID vehicleId,
    UUID driverId,
    String origin,
    String destination,
    Instant plannedStart,
    Integer estimatedDurationMin,
    BigDecimal plannedDistanceKm,
    RouteStatus status,
    Instant startedAt,
    Instant endedAt,
    Integer startOdometerKm,
    Integer endOdometerKm,
    BigDecimal actualDistanceKm,
    String notes) {

  /**
   * Creates a response from a route entity.
   *
   * @param route route entity
   * @return response containing the route data
   */
  public static RouteResponse from(Route route) {
    return new RouteResponse(
        route.getId(),
        route.getVehicleId(),
        route.getDriverId(),
        route.getOrigin(),
        route.getDestination(),
        route.getPlannedStart(),
        route.getEstimatedDurationMin(),
        route.getPlannedDistanceKm(),
        route.getStatus(),
        route.getStartedAt(),
        route.getEndedAt(),
        route.getStartOdometerKm(),
        route.getEndOdometerKm(),
        route.getActualDistanceKm(),
        route.getNotes());
  }
}
