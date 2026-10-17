package com.fleetcontrol.msalerts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fleetcontrol.msalerts.exception.InvalidRequestException;
import com.fleetcontrol.msalerts.models.dto.response.ErrorDetail;
import com.fleetcontrol.msalerts.models.enums.AlertType;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Unit tests of the validation of the parameters of a rule. */
class RuleParamsValidatorTest {

  private final RuleParamsValidator validator = new RuleParamsValidator();

  @Test
  void validate_acceptsTheParametersOfEachType() {
    assertThatCode(
            () -> {
              validator.validate(AlertType.MAINTENANCE_DUE, Map.of("daysBefore", 14));
              validator.validate(AlertType.LICENSE_EXPIRING, Map.of("daysBefore", 365));
              validator.validate(AlertType.HIGH_CONSUMPTION, Map.of("thresholdPercent", 40));
              validator.validate(AlertType.MAINTENANCE_OVERDUE, Map.of());
              validator.validate(AlertType.LICENSE_EXPIRED, null);
            })
        .doesNotThrowAnyException();
  }

  @Test
  void validate_acceptsLongValues() {
    assertThatCode(() -> validator.validate(AlertType.MAINTENANCE_DUE, Map.of("daysBefore", 7L)))
        .doesNotThrowAnyException();
  }

  @Test
  void validate_rejectsParametersInRuleWithNone() {
    assertInvalid(
        AlertType.LICENSE_EXPIRED,
        Map.of("daysBefore", 3),
        "params",
        "this rule has no parameters");
  }

  @Test
  void validate_rejectsAnUnknownParameter() {
    assertInvalid(
        AlertType.MAINTENANCE_DUE,
        Map.of("daysBefore", 3, "hours", 2),
        "params.hours",
        "is not a parameter of this rule");
  }

  @Test
  void validate_rejectsTheParameterOfAnotherType() {
    assertInvalid(
        AlertType.HIGH_CONSUMPTION,
        Map.of("daysBefore", 3),
        "params.daysBefore",
        "is not a parameter of this rule");
  }

  @Test
  void validate_rejectsMissingParameter() {
    assertInvalid(AlertType.MAINTENANCE_DUE, Map.of(), "params.daysBefore", "is required");
    assertInvalid(AlertType.HIGH_CONSUMPTION, null, "params.thresholdPercent", "is required");
  }

  @Test
  void validate_rejectsNullValue() {
    Map<String, Object> params = new HashMap<>();
    params.put("daysBefore", null);

    assertInvalid(AlertType.LICENSE_EXPIRING, params, "params.daysBefore", "is required");
  }

  @Test
  void validate_rejectsValuesThatAreNotIntegers() {
    assertInvalid(
        AlertType.LICENSE_EXPIRING,
        Map.of("daysBefore", 1.5),
        "params.daysBefore",
        "must be an integer");
    assertInvalid(
        AlertType.LICENSE_EXPIRING,
        Map.of("daysBefore", "7"),
        "params.daysBefore",
        "must be an integer");
  }

  @Test
  void validate_rejectsValuesOutOfRange() {
    assertInvalid(
        AlertType.MAINTENANCE_DUE,
        Map.of("daysBefore", 0),
        "params.daysBefore",
        "must be between 1 and 365");
    assertInvalid(
        AlertType.MAINTENANCE_DUE,
        Map.of("daysBefore", 366),
        "params.daysBefore",
        "must be between 1 and 365");
    assertInvalid(
        AlertType.HIGH_CONSUMPTION,
        Map.of("thresholdPercent", 1001),
        "params.thresholdPercent",
        "must be between 1 and 1000");
  }

  private void assertInvalid(
      AlertType type, Map<String, Object> params, String field, String reason) {
    assertThatThrownBy(() -> validator.validate(type, params))
        .isInstanceOfSatisfying(
            InvalidRequestException.class,
            exception ->
                assertThat(exception.details()).containsExactly(new ErrorDetail(field, reason)));
  }
}
