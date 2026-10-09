package com.fleetcontrol.msroutes.client.dto;

import com.fleetcontrol.msroutes.client.enums.VehicleStatus;
import com.fleetcontrol.msroutes.client.enums.VehicleType;
import java.util.UUID;

/**
 * Vehicle data returned by the vehicles service.
 *
 * @param id vehicle identifier
 * @param type vehicle type
 * @param status vehicle status
 * @param odometerKm current odometer reading
 */
public record VehicleResponse(
    UUID id, VehicleType type, VehicleStatus status, Integer odometerKm) {}
