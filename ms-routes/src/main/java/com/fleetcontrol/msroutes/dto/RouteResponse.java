package com.fleetcontrol.msroutes.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fleetcontrol.msroutes.entities.Route;
import com.fleetcontrol.msroutes.enums.RouteStatus;
import io.swagger.v3.oas.annotations.media.Schema;

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
@Schema(description = "A route; fields without a value are omitted")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RouteResponse(
  @Schema(example = "c8e1b7a4-52d9-4f06-8e3b-1a7d90c4f201") UUID id,
  @Schema(example = "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01") UUID vehicleId,
  @Schema(example = "3b2d8f10-7c4a-4e55-b1a0-5d9e2c7f1a01") UUID driverId,
  @Schema(example = "Madrid - Centro Logístico") String origin,
  @Schema(example = "Valencia - Puerto") String destination,
  @Schema(example = "2026-10-06T07:00:00Z") Instant plannedStart,
  @Schema(description = "Estimated duration in minutes", example = "240")
    Integer estimatedDurationMin,
  @Schema(description = "Planned distance in km", example = "355.0") BigDecimal plannedDistanceKm,
  @Schema(example = "PLANNED") RouteStatus status,
  @Schema(example = "2026-10-06T07:04:12Z") Instant startedAt,
  @Schema(example = "2026-10-06T11:20:45Z") Instant endedAt,
  @Schema(example = "45210") Integer startOdometerKm,
  @Schema(example = "45572") Integer endOdometerKm,
  @Schema(description = "Actual distance in km", example = "362.4") BigDecimal actualDistanceKm,
  @Schema(example = "Sin incidencias") String notes) {

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
