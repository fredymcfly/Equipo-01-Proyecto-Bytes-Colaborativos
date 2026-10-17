package com.fleetcontrol.msalerts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fleetcontrol.msalerts.exception.InvalidRequestException;
import com.fleetcontrol.msalerts.exception.RuleNotFoundException;
import com.fleetcontrol.msalerts.models.dto.request.UpdateRuleRequest;
import com.fleetcontrol.msalerts.models.dto.response.RuleResponse;
import com.fleetcontrol.msalerts.models.entities.AlertRule;
import com.fleetcontrol.msalerts.models.enums.AlertSeverity;
import com.fleetcontrol.msalerts.models.enums.AlertType;
import com.fleetcontrol.msalerts.repository.AlertRuleRepository;
import com.fleetcontrol.msalerts.support.Rules;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests of the configuration of the rules. */
@ExtendWith(MockitoExtension.class)
class AlertRuleServiceTest {

  @Mock private AlertRuleRepository repository;

  private AlertRuleService service;

  @BeforeEach
  void setUp() {
    service = new AlertRuleService(repository, new RuleParamsValidator());
  }

  @Test
  void list_returnsTheRulesInTheOrderOfTheirTypes() {
    when(repository.findAll())
        .thenReturn(
            List.of(
                Rules.rule(AlertType.HIGH_CONSUMPTION, "thresholdPercent", 25),
                Rules.rule(AlertType.LICENSE_EXPIRED),
                Rules.rule(AlertType.MAINTENANCE_DUE, "daysBefore", 7)));

    assertThat(service.list())
        .extracting(RuleResponse::type)
        .containsExactly(
            AlertType.MAINTENANCE_DUE, AlertType.LICENSE_EXPIRED, AlertType.HIGH_CONSUMPTION);
  }

  @Test
  void update_replacesEnabledSeverityAndParameters() {
    AlertRule rule = Rules.rule(AlertType.MAINTENANCE_DUE, "daysBefore", 7);
    when(repository.findById(AlertType.MAINTENANCE_DUE)).thenReturn(Optional.of(rule));
    when(repository.saveAndFlush(rule)).thenReturn(rule);

    RuleResponse response =
        service.update(
            "MAINTENANCE_DUE",
            new UpdateRuleRequest(false, AlertSeverity.CRITICAL, Map.of("daysBefore", 14)));

    assertThat(response.enabled()).isFalse();
    assertThat(response.severity()).isEqualTo(AlertSeverity.CRITICAL);
    assertThat(response.params()).containsExactly(Map.entry("daysBefore", 14));
  }

  @Test
  void update_withoutParams_leavesAnEmptyMap() {
    AlertRule rule = Rules.rule(AlertType.LICENSE_EXPIRED);
    when(repository.findById(AlertType.LICENSE_EXPIRED)).thenReturn(Optional.of(rule));
    when(repository.saveAndFlush(rule)).thenReturn(rule);

    RuleResponse response =
        service.update(
            "LICENSE_EXPIRED", new UpdateRuleRequest(true, AlertSeverity.WARNING, null));

    assertThat(response.params()).isEmpty();
  }

  @Test
  void update_withTextThatIsNotType_isNotFound() {
    UpdateRuleRequest request = new UpdateRuleRequest(true, AlertSeverity.INFO, Map.of());

    assertThatThrownBy(() -> service.update("disk_full", request))
        .isInstanceOf(RuleNotFoundException.class);
    verifyNoInteractions(repository);
  }

  @Test
  void update_ofRuleThatIsNotStored_isNotFound() {
    when(repository.findById(AlertType.HIGH_CONSUMPTION)).thenReturn(Optional.empty());
    UpdateRuleRequest request =
        new UpdateRuleRequest(true, AlertSeverity.INFO, Map.of("thresholdPercent", 30));

    assertThatThrownBy(() -> service.update("HIGH_CONSUMPTION", request))
        .isInstanceOf(RuleNotFoundException.class);
  }

  @Test
  void update_withInvalidParams_changesNothing() {
    AlertRule rule = Rules.rule(AlertType.MAINTENANCE_DUE, "daysBefore", 7);
    when(repository.findById(AlertType.MAINTENANCE_DUE)).thenReturn(Optional.of(rule));
    UpdateRuleRequest request =
        new UpdateRuleRequest(false, AlertSeverity.CRITICAL, Map.of("daysBefore", 0));

    assertThatThrownBy(() -> service.update("MAINTENANCE_DUE", request))
        .isInstanceOf(InvalidRequestException.class);

    assertThat(rule.isEnabled()).isTrue();
    assertThat(rule.getSeverity()).isEqualTo(AlertSeverity.WARNING);
    assertThat(rule.getParams()).containsEntry("daysBefore", 7);
    verify(repository, never()).saveAndFlush(rule);
  }
}
