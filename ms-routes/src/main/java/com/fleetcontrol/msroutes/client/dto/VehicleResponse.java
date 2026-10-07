package com.fleetcontrol.msroutes.client.dto;

import com.fleetcontrol.msroutes.client.enums.VehicleStatus;
import com.fleetcontrol.msroutes.client.enums.VehicleType;

import java.util.UUID;

public record VehicleResponse (
  UUID id,
  VehicleType type,
  VehicleStatus status,
  Integer odometerKm
){}
