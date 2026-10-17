package com.fleetcontrol.msalerts.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fleetcontrol.msalerts.models.entities.Alert;
import com.fleetcontrol.msalerts.models.enums.AlertSeverity;
import com.fleetcontrol.msalerts.models.enums.AlertStatus;
import com.fleetcontrol.msalerts.models.enums.AlertType;
import com.fleetcontrol.msalerts.support.AbstractIntegrationTest;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Integration tests for listing and acknowledging alerts. */
class AlertControllerTest extends AbstractIntegrationTest {

  private static final String ALERTS = "/api/alerts";
  private static final Instant NOW = Instant.parse("2026-10-05T06:00:00Z");

  // ------------------------------------------------------------------ list

  @Test
  void list_sortsBySeverityAndThenByNewest() throws Exception {
    Alert info = open(AlertType.HIGH_CONSUMPTION, AlertSeverity.INFO, NOW);
    Alert warningNew = open(AlertType.LICENSE_EXPIRING, AlertSeverity.WARNING, daysAgo(1));
    Alert warningOld = open(AlertType.LICENSE_EXPIRING, AlertSeverity.WARNING, daysAgo(3));
    Alert critical = open(AlertType.MAINTENANCE_OVERDUE, AlertSeverity.CRITICAL, daysAgo(5));

    list("")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(4))
        .andExpect(jsonPath("$.content[0].id").value(critical.getId().toString()))
        .andExpect(jsonPath("$.content[1].id").value(warningNew.getId().toString()))
        .andExpect(jsonPath("$.content[2].id").value(warningOld.getId().toString()))
        .andExpect(jsonPath("$.content[3].id").value(info.getId().toString()));
  }

  @Test
  void list_returnsTheFieldsOfTheAlert() throws Exception {
    UUID vehicleId = UUID.randomUUID();
    saveAlert(
        AlertType.MAINTENANCE_OVERDUE, AlertSeverity.CRITICAL, AlertStatus.OPEN, NOW, vehicleId);

    list("")
        .andExpect(jsonPath("$.content[0].type").value("MAINTENANCE_OVERDUE"))
        .andExpect(jsonPath("$.content[0].severity").value("CRITICAL"))
        .andExpect(jsonPath("$.content[0].status").value("OPEN"))
        .andExpect(jsonPath("$.content[0].vehicleId").value(vehicleId.toString()))
        .andExpect(jsonPath("$.content[0].driverId").isEmpty())
        .andExpect(jsonPath("$.content[0].message").exists())
        .andExpect(jsonPath("$.content[0].createdAt").exists())
        .andExpect(jsonPath("$.content[0].acknowledgedAt").isEmpty())
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(20));
  }

  @Test
  void list_filtersByStatus() throws Exception {
    open(AlertType.LICENSE_EXPIRING, AlertSeverity.WARNING, NOW);
    saveAlert(AlertType.LICENSE_EXPIRED, AlertSeverity.CRITICAL, AlertStatus.RESOLVED, NOW);

    list("?status=OPEN")
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].status").value("OPEN"));
  }

  @Test
  void list_filtersBySeverityAndType() throws Exception {
    open(AlertType.LICENSE_EXPIRING, AlertSeverity.WARNING, NOW);
    open(AlertType.MAINTENANCE_DUE, AlertSeverity.WARNING, NOW);
    open(AlertType.MAINTENANCE_OVERDUE, AlertSeverity.CRITICAL, NOW);

    list("?severity=WARNING").andExpect(jsonPath("$.totalElements").value(2));
    list("?type=MAINTENANCE_DUE")
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].type").value("MAINTENANCE_DUE"));
    list("?severity=WARNING&type=LICENSE_EXPIRING")
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void list_filtersByVehicle() throws Exception {
    UUID vehicleId = UUID.randomUUID();
    saveAlert(AlertType.MAINTENANCE_DUE, AlertSeverity.WARNING, AlertStatus.OPEN, NOW, vehicleId);
    saveAlert(
        AlertType.MAINTENANCE_DUE, AlertSeverity.WARNING, AlertStatus.OPEN, NOW, UUID.randomUUID());

    list("?vehicle=" + vehicleId)
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].vehicleId").value(vehicleId.toString()));
  }

  @Test
  void list_paginatesKeepingTheOrder() throws Exception {
    Alert critical = open(AlertType.MAINTENANCE_OVERDUE, AlertSeverity.CRITICAL, NOW);
    Alert warning = open(AlertType.MAINTENANCE_DUE, AlertSeverity.WARNING, NOW);
    Alert info = open(AlertType.HIGH_CONSUMPTION, AlertSeverity.INFO, NOW);

    list("?page=0&size=2")
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.content[0].id").value(critical.getId().toString()))
        .andExpect(jsonPath("$.content[1].id").value(warning.getId().toString()))
        .andExpect(jsonPath("$.totalElements").value(3))
        .andExpect(jsonPath("$.totalPages").value(2));
    list("?page=1&size=2")
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].id").value(info.getId().toString()))
        .andExpect(jsonPath("$.page").value(1));
  }

  @Test
  void list_withSizeAboveTheMaximum_isRejected() throws Exception {
    list("?size=101")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details[0].field").value("size"));
  }

  @Test
  void list_withUnknownStatus_isRejected() throws Exception {
    list("?status=DONE")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details[0].field").value("status"));
  }

  // ----------------------------------------------------------- acknowledge

  @Test
  void acknowledge_storesTheUserTheDateAndTheComment() throws Exception {
    Alert alert = open(AlertType.MAINTENANCE_OVERDUE, AlertSeverity.CRITICAL, NOW);
    UUID userId = UUID.randomUUID();

    acknowledge(alert.getId(), bearer("MANAGER", userId), "{\"comment\": \"Cita pedida\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(alert.getId().toString()))
        .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"))
        .andExpect(jsonPath("$.acknowledgedAt").exists())
        .andExpect(jsonPath("$.acknowledgedBy").value(userId.toString()))
        .andExpect(jsonPath("$.comment").value("Cita pedida"));

    Alert stored = alertRepository.findById(alert.getId()).orElseThrow();
    assertThat(stored.getStatus()).isEqualTo(AlertStatus.ACKNOWLEDGED);
    assertThat(stored.getAcknowledgedBy()).isEqualTo(userId);
    assertThat(stored.getAcknowledgedAt()).isNotNull();
  }

  @Test
  void acknowledge_withoutBody_isAllowed() throws Exception {
    Alert alert = open(AlertType.LICENSE_EXPIRING, AlertSeverity.WARNING, NOW);

    acknowledge(alert.getId(), bearer("ADMIN"), null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"))
        .andExpect(jsonPath("$.comment").isEmpty());
  }

  @Test
  void acknowledge_unknownAlert_returnsNotFound() throws Exception {
    acknowledge(UUID.randomUUID(), bearer("MANAGER"), null)
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("ALERT_NOT_FOUND"))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  void acknowledge_alreadyAcknowledgedAlert_returnsConflict() throws Exception {
    Alert alert =
        saveAlert(
            AlertType.LICENSE_EXPIRING, AlertSeverity.WARNING, AlertStatus.ACKNOWLEDGED, NOW);

    acknowledge(alert.getId(), bearer("MANAGER"), null)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value("INVALID_ALERT_STATE"));
  }

  @Test
  void acknowledge_resolvedAlert_returnsConflict() throws Exception {
    Alert alert =
        saveAlert(AlertType.LICENSE_EXPIRED, AlertSeverity.CRITICAL, AlertStatus.RESOLVED, NOW);

    acknowledge(alert.getId(), bearer("MANAGER"), null)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value("INVALID_ALERT_STATE"));
  }

  @Test
  void acknowledge_withTooLongComment_isRejected() throws Exception {
    Alert alert = open(AlertType.LICENSE_EXPIRING, AlertSeverity.WARNING, NOW);
    String body = "{\"comment\": \"" + "x".repeat(501) + "\"}";

    acknowledge(alert.getId(), bearer("MANAGER"), body)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details[0].field").value("comment"));
    assertThat(alertRepository.findById(alert.getId()).orElseThrow().getStatus())
        .isEqualTo(AlertStatus.OPEN);
  }

  // --------------------------------------------------------------- helpers

  private static Instant daysAgo(long days) {
    return NOW.minus(Duration.ofDays(days));
  }

  private Alert open(AlertType type, AlertSeverity severity, Instant createdAt) {
    return saveAlert(type, severity, AlertStatus.OPEN, createdAt);
  }

  private ResultActions list(String query) throws Exception {
    return mockMvc.perform(
        get(ALERTS + query).header(HttpHeaders.AUTHORIZATION, bearer("MANAGER")));
  }

  private ResultActions acknowledge(UUID alertId, String authorization, String body)
      throws Exception {
    MockHttpServletRequestBuilder request =
        post(ALERTS + "/{id}/acknowledge", alertId)
            .header(HttpHeaders.AUTHORIZATION, authorization);
    if (body != null) {
      request = request.contentType(MediaType.APPLICATION_JSON).content(body);
    }
    return mockMvc.perform(request);
  }
}
