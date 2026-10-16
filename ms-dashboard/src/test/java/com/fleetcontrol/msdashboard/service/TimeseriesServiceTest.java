package com.fleetcontrol.msdashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fleetcontrol.msdashboard.client.AlertsClient;
import com.fleetcontrol.msdashboard.client.FuelClient;
import com.fleetcontrol.msdashboard.client.MaintenanceClient;
import com.fleetcontrol.msdashboard.client.RoutesClient;
import com.fleetcontrol.msdashboard.models.dto.internal.RoutesStatsDto;
import com.fleetcontrol.msdashboard.models.dto.request.TimeseriesQuery;
import com.fleetcontrol.msdashboard.models.dto.response.MetricSeriesResponse;
import com.fleetcontrol.msdashboard.models.dto.response.TimeseriesResponse;
import com.fleetcontrol.msdashboard.models.enums.DashboardMetric;
import com.fleetcontrol.msdashboard.models.enums.Granularity;
import com.fleetcontrol.msdashboard.support.FeignFailures;
import com.fleetcontrol.msdashboard.support.SourceData;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TimeseriesServiceTest {

  private static final String FROM = "2026-09-07";
  private static final String TO = "2026-09-27";
  private static final String WEEK = "WEEK";

  @Mock private RoutesClient routesClient;
  @Mock private FuelClient fuelClient;
  @Mock private MaintenanceClient maintenanceClient;
  @Mock private AlertsClient alertsClient;

  private TimeseriesService service;

  @BeforeEach
  void setUp() {
    service =
        new TimeseriesService(
            routesClient,
            fuelClient,
            maintenanceClient,
            alertsClient,
            new SourceCaller(Runnable::run));
  }

  private TimeseriesQuery query(DashboardMetric... metrics) {
    return new TimeseriesQuery(
        List.of(metrics), LocalDate.parse(FROM), LocalDate.parse(TO), Granularity.WEEK);
  }

  @Test
  void timeseries_mapsEveryMetricToItsServiceAndUnit() {
    when(routesClient.stats(FROM, TO, WEEK)).thenReturn(SourceData.routesStats());
    when(fuelClient.stats(FROM, TO, WEEK)).thenReturn(SourceData.fuelStats());
    when(maintenanceClient.stats(FROM, TO, WEEK)).thenReturn(SourceData.maintenanceStats());
    when(alertsClient.stats(FROM, TO, WEEK)).thenReturn(SourceData.alertsStats());

    TimeseriesResponse response =
        service.timeseries(
            query(
                DashboardMetric.DISTANCE_KM,
                DashboardMetric.FUEL_LITERS,
                DashboardMetric.FUEL_COST,
                DashboardMetric.CONSUMPTION_L100KM,
                DashboardMetric.MAINTENANCE_COST,
                DashboardMetric.ALERTS_CREATED));

    assertThat(response.partial()).isFalse();
    assertThat(response.unavailable()).isEmpty();
    assertThat(response.granularity()).isEqualTo(Granularity.WEEK);
    assertThat(response.series())
        .extracting(MetricSeriesResponse::metric, MetricSeriesResponse::unit)
        .containsExactly(
            tuple(DashboardMetric.DISTANCE_KM, "km"),
            tuple(DashboardMetric.FUEL_LITERS, "L"),
            tuple(DashboardMetric.FUEL_COST, "EUR"),
            tuple(DashboardMetric.CONSUMPTION_L100KM, "L/100 km"),
            tuple(DashboardMetric.MAINTENANCE_COST, "EUR"),
            tuple(DashboardMetric.ALERTS_CREATED, "alerts"));
    assertThat(response.series().get(0).points().get(0).period()).isEqualTo(FROM);
    assertThat(response.series().get(0).points().get(0).value()).isEqualByComparingTo("9034.1");
    assertThat(response.series().get(1).points().get(1).value()).isEqualByComparingTo("1077.9");
    assertThat(response.series().get(2).points().get(0).value()).isEqualByComparingTo("1595.21");
    assertThat(response.series().get(3).points().get(1).value()).isEqualByComparingTo("11.1");
    assertThat(response.series().get(4).points().get(1).value()).isEqualByComparingTo("715.25");
    assertThat(response.series().get(5).points().get(1).value()).isEqualByComparingTo("5");
  }

  @Test
  void timeseries_callsEachServiceOnceEvenWithSeveralMetricsOfIt() {
    when(fuelClient.stats(FROM, TO, WEEK)).thenReturn(SourceData.fuelStats());

    service.timeseries(query(DashboardMetric.FUEL_LITERS, DashboardMetric.FUEL_COST));

    verify(fuelClient).stats(FROM, TO, WEEK);
    verifyNoInteractions(routesClient, maintenanceClient, alertsClient);
  }

  @Test
  void timeseries_whenServiceFails_leavesOutItsMetricsAndReportsItOnce() {
    when(routesClient.stats(FROM, TO, WEEK)).thenReturn(SourceData.routesStats());
    when(fuelClient.stats(FROM, TO, WEEK)).thenThrow(FeignFailures.unavailable());

    TimeseriesResponse response =
        service.timeseries(
            query(
                DashboardMetric.DISTANCE_KM,
                DashboardMetric.FUEL_LITERS,
                DashboardMetric.FUEL_COST));

    assertThat(response.partial()).isTrue();
    assertThat(response.unavailable()).containsExactly("ms-fuel");
    assertThat(response.series())
        .extracting(MetricSeriesResponse::metric)
        .containsExactly(DashboardMetric.DISTANCE_KM);
  }

  @Test
  void timeseries_whenEveryServiceFails_returnsNoSeries() {
    when(routesClient.stats(FROM, TO, WEEK)).thenThrow(FeignFailures.unavailable());
    when(alertsClient.stats(FROM, TO, WEEK)).thenThrow(FeignFailures.unavailable());

    TimeseriesResponse response =
        service.timeseries(query(DashboardMetric.DISTANCE_KM, DashboardMetric.ALERTS_CREATED));

    assertThat(response.partial()).isTrue();
    assertThat(response.unavailable()).containsExactly("ms-routes", "ms-alerts");
    assertThat(response.series()).isEmpty();
  }

  @Test
  void timeseries_withoutPoints_returnsAnEmptySeries() {
    when(routesClient.stats(FROM, TO, WEEK))
        .thenReturn(new RoutesStatsDto(new RoutesStatsDto.Totals(0, BigDecimal.ZERO), null));

    TimeseriesResponse response = service.timeseries(query(DashboardMetric.DISTANCE_KM));

    assertThat(response.partial()).isFalse();
    assertThat(response.series()).hasSize(1);
    assertThat(response.series().get(0).points()).isEmpty();
  }
}
