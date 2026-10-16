package com.fleetcontrol.msdashboard.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fleetcontrol.msdashboard.exception.InvalidRequestException;
import com.fleetcontrol.msdashboard.models.dto.request.TimeseriesQuery;
import com.fleetcontrol.msdashboard.models.enums.DashboardMetric;
import com.fleetcontrol.msdashboard.models.enums.Granularity;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class TimeseriesQueryTest {

  private static final LocalDate FROM = LocalDate.parse("2026-01-01");
  private static final List<DashboardMetric> METRICS = List.of(DashboardMetric.DISTANCE_KM);

  private TimeseriesQuery query(List<DashboardMetric> metrics, LocalDate to) {
    return new TimeseriesQuery(metrics, FROM, to, Granularity.DAY);
  }

  @Test
  void create_withRangeOfOneDay_isValid() {
    assertThat(query(METRICS, FROM).to()).isEqualTo(FROM);
  }

  @Test
  void create_withRangeOf366Days_isValid() {
    assertThat(query(METRICS, LocalDate.parse("2027-01-01")).metrics()).isEqualTo(METRICS);
  }

  @Test
  void create_withRangeOf367Days_isRejected() {
    LocalDate to = LocalDate.parse("2027-01-02");

    assertThatThrownBy(() -> query(METRICS, to))
        .isInstanceOf(InvalidRequestException.class)
        .satisfies(
            exception -> {
              InvalidRequestException invalid = (InvalidRequestException) exception;
              assertThat(invalid.status().value()).isEqualTo(400);
              assertThat(invalid.code()).isEqualTo("VALIDATION_ERROR");
              assertThat(invalid.details()).hasSize(1);
              assertThat(invalid.details().get(0).field()).isEqualTo("to");
            });
  }

  @Test
  void create_withAnEndBeforeTheStart_isRejected() {
    LocalDate to = LocalDate.parse("2025-12-31");

    assertThatThrownBy(() -> query(METRICS, to)).isInstanceOf(InvalidRequestException.class);
  }

  @Test
  void create_withoutMetrics_isRejected() {
    assertThatThrownBy(() -> query(List.of(), FROM))
        .isInstanceOf(InvalidRequestException.class)
        .satisfies(
            exception ->
                assertThat(((InvalidRequestException) exception).details().get(0).field())
                    .isEqualTo("metrics"));
  }

  @Test
  void requires_isTrueOnlyForTheServicesOfTheMetrics() {
    TimeseriesQuery query =
        query(List.of(DashboardMetric.FUEL_COST, DashboardMetric.ALERTS_CREATED), FROM);

    assertThat(query.requires("ms-fuel")).isTrue();
    assertThat(query.requires("ms-alerts")).isTrue();
    assertThat(query.requires("ms-routes")).isFalse();
    assertThat(query.requires("ms-maintenance")).isFalse();
  }
}
