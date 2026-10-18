package com.fleetcontrol.msalerts.service.detector;

import com.fleetcontrol.msalerts.models.dto.internal.Detection;
import com.fleetcontrol.msalerts.models.entities.AlertRule;
import com.fleetcontrol.msalerts.models.enums.AlertType;
import feign.FeignException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;

/** Looks for the conditions of the alert types that depend on the same source service. */
public interface AlertDetector {

  /** Alert types this detector evaluates. */
  Set<AlertType> types();

  /**
   * Returns the conditions that hold today.
   *
   * @param rules the enabled rules among {@link #types()}, never empty
   * @param today the current day in UTC
   * @param plates gives the plate of a vehicle, or its id if the plate is not available
   * @throws FeignException if a source service does not answer
   * @throws NoFallbackAvailableException if the circuit breaker of a source service is open
   */
  List<Detection> detect(
      Map<AlertType, AlertRule> rules, LocalDate today, Function<UUID, String> plates);
}
