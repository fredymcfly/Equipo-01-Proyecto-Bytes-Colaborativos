package com.fleetcontrol.msalerts.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fleetcontrol.msalerts.models.enums.AlertSeverity;
import com.fleetcontrol.msalerts.models.enums.AlertStatus;
import com.fleetcontrol.msalerts.models.enums.AlertType;
import com.fleetcontrol.msalerts.support.AbstractIntegrationTest;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultActions;

/** Integration tests of the alert statistics. */
class StatsControllerTest extends AbstractIntegrationTest {

  private static final String STATS = "/api/alerts/stats";

  @BeforeEach
  void saveAlerts() {
    created("2026-09-07T10:00:00Z", AlertSeverity.WARNING, AlertStatus.RESOLVED);
    created("2026-09-08T23:30:00Z", AlertSeverity.CRITICAL, AlertStatus.RESOLVED);
    created("2026-09-15T00:00:00Z", AlertSeverity.WARNING, AlertStatus.RESOLVED);
    created("2026-09-27T23:59:59Z", AlertSeverity.INFO, AlertStatus.RESOLVED);
    created("2026-09-28T00:00:00Z", AlertSeverity.CRITICAL, AlertStatus.RESOLVED);
  }

  @Test
  void stats_byDay_includesTheEmptyDays() throws Exception {
    stats("?from=2026-09-07&to=2026-09-09&granularity=DAY")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.from").value("2026-09-07"))
        .andExpect(jsonPath("$.to").value("2026-09-09"))
        .andExpect(jsonPath("$.granularity").value("DAY"))
        .andExpect(jsonPath("$.series.length()").value(3))
        .andExpect(jsonPath("$.series[0].period").value("2026-09-07"))
        .andExpect(jsonPath("$.series[0].created").value(1))
        .andExpect(jsonPath("$.series[0].bySeverity.WARNING").value(1))
        .andExpect(jsonPath("$.series[1].period").value("2026-09-08"))
        .andExpect(jsonPath("$.series[1].bySeverity.CRITICAL").value(1))
        .andExpect(jsonPath("$.series[2].period").value("2026-09-09"))
        .andExpect(jsonPath("$.series[2].created").value(0))
        .andExpect(jsonPath("$.series[2].bySeverity.INFO").value(0))
        .andExpect(jsonPath("$.series[2].bySeverity.WARNING").value(0))
        .andExpect(jsonPath("$.series[2].bySeverity.CRITICAL").value(0))
        .andExpect(jsonPath("$.totals.created").value(2));
  }

  @Test
  void stats_isDayByDefault() throws Exception {
    stats("?from=2026-09-07&to=2026-09-08")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.granularity").value("DAY"))
        .andExpect(jsonPath("$.series.length()").value(2));
  }

  @Test
  void stats_byWeek_startsOnMonday() throws Exception {
    stats("?from=2026-09-07&to=2026-09-27&granularity=WEEK")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.series.length()").value(3))
        .andExpect(jsonPath("$.series[0].period").value("2026-09-07"))
        .andExpect(jsonPath("$.series[0].created").value(2))
        .andExpect(jsonPath("$.series[0].bySeverity.WARNING").value(1))
        .andExpect(jsonPath("$.series[0].bySeverity.CRITICAL").value(1))
        .andExpect(jsonPath("$.series[1].period").value("2026-09-14"))
        .andExpect(jsonPath("$.series[1].created").value(1))
        .andExpect(jsonPath("$.series[2].period").value("2026-09-21"))
        .andExpect(jsonPath("$.series[2].bySeverity.INFO").value(1))
        .andExpect(jsonPath("$.totals.created").value(4));
  }

  @Test
  void stats_byWeek_startsTheFirstPeriodOnTheMondayOfTheFirstDay() throws Exception {
    stats("?from=2026-09-10&to=2026-09-16&granularity=WEEK")
        .andExpect(jsonPath("$.series.length()").value(2))
        .andExpect(jsonPath("$.series[0].period").value("2026-09-07"))
        .andExpect(jsonPath("$.series[1].period").value("2026-09-14"))
        .andExpect(jsonPath("$.totals.created").value(1));
  }

  @Test
  void stats_byMonth_startsOnTheFirstDay() throws Exception {
    stats("?from=2026-08-15&to=2026-10-10&granularity=MONTH")
        .andExpect(jsonPath("$.series.length()").value(3))
        .andExpect(jsonPath("$.series[0].period").value("2026-08-01"))
        .andExpect(jsonPath("$.series[0].created").value(0))
        .andExpect(jsonPath("$.series[1].period").value("2026-09-01"))
        .andExpect(jsonPath("$.series[1].created").value(5))
        .andExpect(jsonPath("$.series[2].period").value("2026-10-01"))
        .andExpect(jsonPath("$.series[2].created").value(0))
        .andExpect(jsonPath("$.totals.created").value(5));
  }

  @Test
  void stats_countsTheOpenAlertsBySeverity() throws Exception {
    alertRepository.deleteAll();
    created("2026-09-07T10:00:00Z", AlertSeverity.WARNING, AlertStatus.OPEN);
    created("2026-09-07T11:00:00Z", AlertSeverity.WARNING, AlertStatus.OPEN);
    created("2026-09-07T12:00:00Z", AlertSeverity.CRITICAL, AlertStatus.OPEN);
    created("2026-09-07T13:00:00Z", AlertSeverity.CRITICAL, AlertStatus.ACKNOWLEDGED);
    created("2026-09-07T14:00:00Z", AlertSeverity.INFO, AlertStatus.RESOLVED);

    stats("?from=2026-09-07&to=2026-09-07")
        .andExpect(jsonPath("$.open.INFO").value(0))
        .andExpect(jsonPath("$.open.WARNING").value(2))
        .andExpect(jsonPath("$.open.CRITICAL").value(1))
        .andExpect(jsonPath("$.totals.created").value(5));
  }

  @Test
  void stats_withoutAlerts_returnsZeros() throws Exception {
    alertRepository.deleteAll();

    stats("?from=2026-09-07&to=2026-09-08")
        .andExpect(jsonPath("$.totals.created").value(0))
        .andExpect(jsonPath("$.series[0].created").value(0))
        .andExpect(jsonPath("$.open.CRITICAL").value(0));
  }

  @Test
  void stats_withInternalKey_isAllowed() throws Exception {
    mockMvc
        .perform(
            get(STATS + "?from=2026-09-07&to=2026-09-08").header(INTERNAL_KEY_HEADER, INTERNAL_KEY))
        .andExpect(status().isOk());
  }

  @Test
  void stats_withToBeforeFrom_isRejected() throws Exception {
    stats("?from=2026-09-10&to=2026-09-09")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details[0].field").value("to"));
  }

  @Test
  void stats_withMoreThan366Days_isRejected() throws Exception {
    stats("?from=2025-09-01&to=2026-09-02")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details[0].field").value("to"));
  }

  @Test
  void stats_withExactly366Days_isAllowed() throws Exception {
    stats("?from=2025-09-01&to=2026-09-01&granularity=MONTH").andExpect(status().isOk());
  }

  @Test
  void stats_withoutFrom_isRejected() throws Exception {
    stats("?to=2026-09-09")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details[0].field").value("from"));
  }

  @Test
  void stats_withInvalidDate_isRejected() throws Exception {
    stats("?from=2026-13-01&to=2026-09-09")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details[0].field").value("from"));
  }

  @Test
  void stats_withUnknownGranularity_isRejected() throws Exception {
    stats("?from=2026-09-01&to=2026-09-09&granularity=YEAR")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details[0].field").value("granularity"));
  }

  private void created(String createdAt, AlertSeverity severity, AlertStatus status) {
    saveAlert(AlertType.MAINTENANCE_DUE, severity, status, Instant.parse(createdAt));
  }

  private ResultActions stats(String query) throws Exception {
    return mockMvc.perform(get(STATS + query).header(HttpHeaders.AUTHORIZATION, bearer("MANAGER")));
  }
}
