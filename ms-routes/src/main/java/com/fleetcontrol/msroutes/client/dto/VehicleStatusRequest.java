package com.fleetcontrol.msroutes.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fleetcontrol.msroutes.client.enums.VehicleStatus;

/**
 * Body sent to ms-vehicles to change the status of a vehicle.
 *
 * @param status new status of the vehicle
 * @param odometerKm new odometer reading; omitted when it does not change
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record VehicleStatusRequest(VehicleStatus status, Integer odometerKm) {}
