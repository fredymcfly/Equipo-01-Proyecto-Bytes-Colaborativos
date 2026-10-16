package com.fleetcontrol.msdashboard.models.dto.internal;

import java.util.UUID;

/** Part of the answer of {@code GET /api/vehicles/{id}} in ms-vehicles that the dashboard uses. */
public record VehicleDto(UUID id, String plate) {}
