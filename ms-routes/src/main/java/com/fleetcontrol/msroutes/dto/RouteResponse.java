package com.fleetcontrol.msroutes.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fleetcontrol.msroutes.entities.Route;
import com.fleetcontrol.msroutes.enums.RouteStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Route as exposed by the API, fields without value are omitted
 * @param id
 * @param vehicleId
 * @param driverId
 * @param origin
 * @param destination
 * @param plannedStart
 * @param estimatedDurationMin
 * @param plannedDistanceKm
 * @param status
 * @param startedAt
 * @param endedAt
 * @param startOdometerKm
 * @param endOdometerKm
 * @param actualDistanceKm
 * @param notes
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
   String notes
) {

  public static RouteResponse from(Route route){
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
      route.getNotes()
    );
  }
}
