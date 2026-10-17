package com.fleetcontrol.msalerts.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fleetcontrol.msalerts.models.entities.AlertRule;
import com.fleetcontrol.msalerts.models.enums.AlertSeverity;
import com.fleetcontrol.msalerts.models.enums.AlertType;
import com.fleetcontrol.msalerts.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** Integration tests for the configuration of the rules. */
class AlertRuleControllerTest extends AbstractIntegrationTest {

  private static final String RULES = "/api/alerts/rules";

  @Test
  void list_returnsTheFiveRulesWithTheirDefaults() throws Exception {
    mockMvc
        .perform(get(RULES).header(HttpHeaders.AUTHORIZATION, bearer("MANAGER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(5))
        .andExpect(jsonPath("$[0].type").value("MAINTENANCE_DUE"))
        .andExpect(jsonPath("$[0].enabled").value(true))
        .andExpect(jsonPath("$[0].severity").value("WARNING"))
        .andExpect(jsonPath("$[0].params.daysBefore").value(7))
        .andExpect(jsonPath("$[1].type").value("MAINTENANCE_OVERDUE"))
        .andExpect(jsonPath("$[1].severity").value("CRITICAL"))
        .andExpect(jsonPath("$[1].params").isEmpty())
        .andExpect(jsonPath("$[2].type").value("LICENSE_EXPIRING"))
        .andExpect(jsonPath("$[2].params.daysBefore").value(30))
        .andExpect(jsonPath("$[3].type").value("LICENSE_EXPIRED"))
        .andExpect(jsonPath("$[4].type").value("HIGH_CONSUMPTION"))
        .andExpect(jsonPath("$[4].params.thresholdPercent").value(25));
  }

  @Test
  void update_changesTheRule() throws Exception {
    update(
            "MAINTENANCE_DUE",
            "{\"enabled\": false, \"severity\": \"CRITICAL\", \"params\": {\"daysBefore\": 14}}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.type").value("MAINTENANCE_DUE"))
        .andExpect(jsonPath("$.enabled").value(false))
        .andExpect(jsonPath("$.severity").value("CRITICAL"))
        .andExpect(jsonPath("$.params.daysBefore").value(14));

    AlertRule stored = ruleRepository.findById(AlertType.MAINTENANCE_DUE).orElseThrow();
    assertThat(stored.isEnabled()).isFalse();
    assertThat(stored.getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
    assertThat(stored.getParams()).containsEntry("daysBefore", 14);
  }

  @Test
  void update_ruleWithoutParameters_acceptsMissingParams() throws Exception {
    update("LICENSE_EXPIRED", "{\"enabled\": true, \"severity\": \"WARNING\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.severity").value("WARNING"))
        .andExpect(jsonPath("$.params").isEmpty());
  }

  @Test
  void update_unknownType_returnsNotFound() throws Exception {
    update("DISK_FULL", "{\"enabled\": true, \"severity\": \"WARNING\"}")
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("RULE_NOT_FOUND"));
  }

  @Test
  void update_withUnknownParameter_isRejected() throws Exception {
    update(
            "MAINTENANCE_DUE",
            "{\"enabled\": true, \"severity\": \"WARNING\", \"params\": {\"hours\": 3}}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details[0].field").value("params.hours"));
  }

  @Test
  void update_withParameterOfAnotherRule_isRejected() throws Exception {
    update(
            "HIGH_CONSUMPTION",
            "{\"enabled\": true, \"severity\": \"WARNING\", \"params\": {\"daysBefore\": 5}}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details[0].field").value("params.daysBefore"));
  }

  @Test
  void update_withParametersInRuleWithNone_isRejected() throws Exception {
    update(
            "MAINTENANCE_OVERDUE",
            "{\"enabled\": true, \"severity\": \"CRITICAL\", \"params\": {\"daysBefore\": 5}}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details[0].field").value("params"));
  }

  @Test
  void update_withNonIntegerValue_isRejected() throws Exception {
    update(
            "LICENSE_EXPIRING",
            "{\"enabled\": true, \"severity\": \"WARNING\", \"params\": {\"daysBefore\": 1.5}}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details[0].field").value("params.daysBefore"));
    update(
            "LICENSE_EXPIRING",
            "{\"enabled\": true, \"severity\": \"WARNING\", \"params\": {\"daysBefore\": \"7\"}}")
        .andExpect(status().isBadRequest());
  }

  @Test
  void update_withValueOutOfRange_isRejected() throws Exception {
    update(
            "LICENSE_EXPIRING",
            "{\"enabled\": true, \"severity\": \"WARNING\", \"params\": {\"daysBefore\": 0}}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details[0].field").value("params.daysBefore"));
    update(
            "HIGH_CONSUMPTION",
            "{\"enabled\": true, \"severity\": \"WARNING\","
                + " \"params\": {\"thresholdPercent\": 5000}}")
        .andExpect(status().isBadRequest());
  }

  @Test
  void update_withoutRequiredParameter_isRejected() throws Exception {
    update("MAINTENANCE_DUE", "{\"enabled\": true, \"severity\": \"WARNING\", \"params\": {}}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details[0].field").value("params.daysBefore"));
  }

  @Test
  void update_withoutEnabled_isRejected() throws Exception {
    update("MAINTENANCE_DUE", "{\"severity\": \"WARNING\", \"params\": {\"daysBefore\": 7}}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details[0].field").value("enabled"));
  }

  @Test
  void update_withUnknownSeverity_isRejected() throws Exception {
    update(
            "MAINTENANCE_DUE",
            "{\"enabled\": true, \"severity\": \"SEVERE\", \"params\": {\"daysBefore\": 7}}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
  }

  @Test
  void failedUpdate_doesNotChangeTheRule() throws Exception {
    update(
            "MAINTENANCE_DUE",
            "{\"enabled\": false, \"severity\": \"CRITICAL\", \"params\": {\"daysBefore\": 0}}")
        .andExpect(status().isBadRequest());

    AlertRule stored = ruleRepository.findById(AlertType.MAINTENANCE_DUE).orElseThrow();
    assertThat(stored.isEnabled()).isTrue();
    assertThat(stored.getParams()).containsEntry("daysBefore", 7);
  }

  private ResultActions update(String type, String body) throws Exception {
    return mockMvc.perform(
        put(RULES + "/{type}", type)
            .header(HttpHeaders.AUTHORIZATION, bearer("ADMIN"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }
}
