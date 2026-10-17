package com.fleetcontrol.msalerts.models.dto.internal;

import java.util.UUID;

/** Part of the answer of {@code GET /api/vehicles/{id}} in ms-vehicles that the evaluator uses. */
public record VehicleDto(UUID id, String plate) {}
