package com.fleetcontrol.msalerts.models.dto.internal;

import com.fleetcontrol.msalerts.models.enums.AlertSeverity;
import com.fleetcontrol.msalerts.models.enums.AlertType;
import java.util.UUID;

/**
 * A condition found by a detector, which becomes an alert if there is not one already.
 *
 * @param dedupKey identifies the condition, so it is not reported twice while it lasts
 */
public record Detection(
    AlertType type,
    AlertSeverity severity,
    UUID vehicleId,
    UUID driverId,
    String message,
    String dedupKey) {}
