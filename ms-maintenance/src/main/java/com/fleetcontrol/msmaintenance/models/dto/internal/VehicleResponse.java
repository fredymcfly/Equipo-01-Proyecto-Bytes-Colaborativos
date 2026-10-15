package com.fleetcontrol.msmaintenance.models.dto.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

/** The data of a vehicle that this service needs from ms-vehicles. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record VehicleResponse(UUID id, String status, Integer odometerKm) {}
