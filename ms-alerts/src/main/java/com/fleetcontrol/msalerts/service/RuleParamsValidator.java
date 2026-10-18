package com.fleetcontrol.msalerts.service;

import com.fleetcontrol.msalerts.constants.BusinessConstants;
import com.fleetcontrol.msalerts.constants.ErrorConstants;
import com.fleetcontrol.msalerts.exception.InvalidRequestException;
import com.fleetcontrol.msalerts.models.enums.AlertType;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Checks that the parameters of a rule are the ones its type accepts, with valid values. */
@Component
public class RuleParamsValidator {

  private static final Bounds DAYS_BEFORE =
      new Bounds(
          BusinessConstants.RuleParams.MIN_DAYS_BEFORE,
          BusinessConstants.RuleParams.MAX_DAYS_BEFORE);
  private static final Bounds THRESHOLD =
      new Bounds(
          BusinessConstants.RuleParams.MIN_THRESHOLD_PERCENT,
          BusinessConstants.RuleParams.MAX_THRESHOLD_PERCENT);

  /** Parameters each type accepts. All of them are required integers. */
  private static final Map<AlertType, Map<String, Bounds>> ACCEPTED =
      Map.of(
          AlertType.MAINTENANCE_DUE, Map.of(BusinessConstants.RuleParams.DAYS_BEFORE, DAYS_BEFORE),
          AlertType.MAINTENANCE_OVERDUE, Map.of(),
          AlertType.LICENSE_EXPIRING, Map.of(BusinessConstants.RuleParams.DAYS_BEFORE, DAYS_BEFORE),
          AlertType.LICENSE_EXPIRED, Map.of(),
          AlertType.HIGH_CONSUMPTION,
              Map.of(BusinessConstants.RuleParams.THRESHOLD_PERCENT, THRESHOLD));

  /** Range of values of a parameter. */
  private record Bounds(int min, int max) {}

  /**
   * Validates the parameters of a rule of the given type.
   *
   * @throws InvalidRequestException with the first parameter that is not valid
   */
  public void validate(AlertType type, Map<String, Object> params) {
    Map<String, Object> given = params == null ? Map.of() : params;
    Map<String, Bounds> accepted = ACCEPTED.get(type);
    if (accepted.isEmpty() && !given.isEmpty()) {
      throw new InvalidRequestException(
          BusinessConstants.Fields.PARAMS, ErrorConstants.Reason.NO_PARAMS_ALLOWED);
    }
    for (String name : given.keySet()) {
      if (!accepted.containsKey(name)) {
        throw new InvalidRequestException(field(name), ErrorConstants.Reason.UNKNOWN_PARAM);
      }
    }
    accepted.forEach((name, bounds) -> checkValue(name, given.get(name), bounds));
  }

  private void checkValue(String name, Object value, Bounds bounds) {
    if (value == null) {
      throw new InvalidRequestException(field(name), ErrorConstants.Reason.REQUIRED);
    }
    if (!(value instanceof Integer || value instanceof Long)) {
      throw new InvalidRequestException(field(name), ErrorConstants.Reason.NOT_INTEGER);
    }
    long number = ((Number) value).longValue();
    if (number < bounds.min() || number > bounds.max()) {
      throw new InvalidRequestException(
          field(name),
          String.format(ErrorConstants.Reason.OUT_OF_RANGE, bounds.min(), bounds.max()));
    }
  }

  private String field(String name) {
    return BusinessConstants.Fields.PARAMS + "." + name;
  }
}
