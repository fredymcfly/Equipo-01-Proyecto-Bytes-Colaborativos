package com.fleetcontrol.msdashboard.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fleetcontrol.msdashboard.models.dto.internal.FuelConsumptionDto;
import com.fleetcontrol.msdashboard.support.AbstractApiTest;
import com.fleetcontrol.msdashboard.support.FeignFailures;
import com.fleetcontrol.msdashboard.support.SourceData;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultActions;

/** Behaviour of the three dashboard endpoints, with the source services mocked. */
class DashboardControllerTest extends AbstractApiTest {

  private static final String DASHBOARD = "/api/dashboard";
  private static final String TIMESERIES = "/api/dashboard/timeseries";
  private static final String RANKING = "/api/dashboard/vehicles/ranking";
  private static final String FROM = "2026-09-07";
  private static final String TO = "2026-09-27";
  private static final String DAY = "DAY";
  private static final String UNAVAILABLE = "$.unavailable";
  private static final String FUEL = "ms-fuel";
  private static final String ERROR = "$.error";
  private static final String VALIDATION_ERROR = "VALIDATION_ERROR";

  private ResultActions getAsManager(String url) throws Exception {
    return mockMvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, bearer("MANAGER")));
  }

  // ---------------------------------------------------------------- overview

  @Test
  void overview_withEverySourceUp_returnsCompleteSummary() throws Exception {
    getAsManager(DASHBOARD)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.partial").value(false))
        .andExpect(jsonPath(UNAVAILABLE, hasSize(0)))
        .andExpect(jsonPath("$.generatedAt").exists())
        .andExpect(jsonPath("$.period.from").exists())
        .andExpect(jsonPath("$.period.to").exists())
        .andExpect(jsonPath("$.fleet.total").value(25))
        .andExpect(jsonPath("$.fleet.available").value(14))
        .andExpect(jsonPath("$.fleet.inUse").value(8))
        .andExpect(jsonPath("$.fleet.inMaintenance").value(2))
        .andExpect(jsonPath("$.fleet.outOfService").value(1))
        .andExpect(jsonPath("$.kpis.routesCompleted").value(412))
        .andExpect(jsonPath("$.kpis.distanceKm").value(38420.5))
        .andExpect(jsonPath("$.kpis.fuelLiters").value(4226.3))
        .andExpect(jsonPath("$.kpis.fuelCost").value(6846.61))
        .andExpect(jsonPath("$.kpis.avgConsumptionL100km").value(11.0))
        .andExpect(jsonPath("$.kpis.maintenanceCost").value(2140.25))
        .andExpect(jsonPath("$.alerts.open").value(11))
        .andExpect(jsonPath("$.alerts.critical").value(2))
        .andExpect(jsonPath("$.alerts.warning").value(9))
        .andExpect(jsonPath("$.alerts.info").value(0))
        .andExpect(jsonPath("$.upcomingMaintenance", hasSize(1)))
        .andExpect(
            jsonPath("$.upcomingMaintenance[0].orderId").value(SourceData.ORDER_ID.toString()))
        .andExpect(jsonPath("$.upcomingMaintenance[0].plate").value(SourceData.VAN_PLATE))
        .andExpect(jsonPath("$.upcomingMaintenance[0].type").value("OIL_CHANGE"))
        .andExpect(jsonPath("$.upcomingMaintenance[0].scheduledFor").value("2026-10-08"));
  }

  @Test
  void overview_whenFuelFails_isPartialAndItsValuesAreNull() throws Exception {
    when(fuelClient.stats(anyString(), anyString(), anyString()))
        .thenThrow(FeignFailures.unavailable());

    getAsManager(DASHBOARD)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.partial").value(true))
        .andExpect(jsonPath(UNAVAILABLE, hasSize(1)))
        .andExpect(jsonPath("$.unavailable[0]").value(FUEL))
        .andExpect(jsonPath("$.kpis.fuelLiters").value(nullValue()))
        .andExpect(jsonPath("$.kpis.fuelCost").value(nullValue()))
        .andExpect(jsonPath("$.kpis.avgConsumptionL100km").value(nullValue()))
        .andExpect(jsonPath("$.kpis.routesCompleted").value(412))
        .andExpect(jsonPath("$.kpis.maintenanceCost").value(2140.25))
        .andExpect(jsonPath("$.fleet.total").value(25));
  }

  @Test
  void overview_whenVehiclesFail_fleetIsNullAndPlatesAreNotLookedUp() throws Exception {
    when(vehiclesClient.summary()).thenThrow(FeignFailures.unavailable());

    getAsManager(DASHBOARD)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.partial").value(true))
        .andExpect(jsonPath("$.unavailable[0]").value("ms-vehicles"))
        .andExpect(jsonPath("$.fleet").value(nullValue()))
        .andExpect(jsonPath("$.upcomingMaintenance[0].plate").value(nullValue()))
        .andExpect(jsonPath("$.upcomingMaintenance[0].type").value("OIL_CHANGE"));

    verify(vehiclesClient, never()).getVehicle(any());
  }

  @Test
  void overview_whenOrdersFail_upcomingMaintenanceIsNull() throws Exception {
    when(maintenanceClient.orders(anyString(), anyInt(), anyInt()))
        .thenThrow(FeignFailures.unavailable());

    getAsManager(DASHBOARD)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.partial").value(true))
        .andExpect(jsonPath("$.unavailable[0]").value("ms-maintenance"))
        .andExpect(jsonPath("$.upcomingMaintenance").value(nullValue()))
        .andExpect(jsonPath("$.kpis.maintenanceCost").value(2140.25));
  }

  @Test
  void overview_whenPlateLookupFails_plateIsNullButTheSummaryIsComplete() throws Exception {
    when(vehiclesClient.getVehicle(SourceData.VAN_ID)).thenThrow(FeignFailures.unavailable());

    getAsManager(DASHBOARD)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.partial").value(false))
        .andExpect(jsonPath("$.upcomingMaintenance[0].plate").value(nullValue()));
  }

  @Test
  void overview_whenEverySourceFails_stillAnswersOk() throws Exception {
    when(vehiclesClient.summary()).thenThrow(FeignFailures.unavailable());
    when(routesClient.stats(anyString(), anyString(), anyString()))
        .thenThrow(FeignFailures.unavailable());
    when(fuelClient.stats(anyString(), anyString(), anyString()))
        .thenThrow(FeignFailures.unavailable());
    when(maintenanceClient.stats(anyString(), anyString(), anyString()))
        .thenThrow(FeignFailures.unavailable());
    when(maintenanceClient.orders(anyString(), anyInt(), anyInt()))
        .thenThrow(FeignFailures.unavailable());
    when(alertsClient.stats(anyString(), anyString(), anyString()))
        .thenThrow(FeignFailures.unavailable());

    getAsManager(DASHBOARD)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.partial").value(true))
        .andExpect(jsonPath(UNAVAILABLE, hasSize(5)))
        .andExpect(jsonPath("$.fleet").value(nullValue()))
        .andExpect(jsonPath("$.alerts").value(nullValue()))
        .andExpect(jsonPath("$.upcomingMaintenance").value(nullValue()))
        .andExpect(jsonPath("$.kpis.routesCompleted").value(nullValue()));
  }

  @Test
  void overview_withDaysOutOfRange_isRejected() throws Exception {
    getAsManager(DASHBOARD + "?days=0")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath(ERROR).value(VALIDATION_ERROR))
        .andExpect(jsonPath("$.details[0].field").value("days"));
    getAsManager(DASHBOARD + "?days=366").andExpect(status().isBadRequest());
    getAsManager(DASHBOARD + "?days=abc")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath(ERROR).value(VALIDATION_ERROR));
  }

  @Test
  void overview_withLimitDays_isAccepted() throws Exception {
    getAsManager(DASHBOARD + "?days=1").andExpect(status().isOk());
    getAsManager(DASHBOARD + "?days=365").andExpect(status().isOk());
  }

  // -------------------------------------------------------------- timeseries

  @Test
  void timeseries_returnsOneSeriesPerMetricInTheRequestedOrder() throws Exception {
    getAsManager(TIMESERIES + "?metrics=FUEL_COST,DISTANCE_KM&from=" + FROM + "&to=" + TO)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.from").value(FROM))
        .andExpect(jsonPath("$.to").value(TO))
        .andExpect(jsonPath("$.granularity").value(DAY))
        .andExpect(jsonPath("$.partial").value(false))
        .andExpect(jsonPath("$.series", hasSize(2)))
        .andExpect(jsonPath("$.series[0].metric").value("FUEL_COST"))
        .andExpect(jsonPath("$.series[0].unit").value("EUR"))
        .andExpect(jsonPath("$.series[0].points[0].period").value(FROM))
        .andExpect(jsonPath("$.series[0].points[0].value").value(1595.21))
        .andExpect(jsonPath("$.series[1].metric").value("DISTANCE_KM"))
        .andExpect(jsonPath("$.series[1].unit").value("km"))
        .andExpect(jsonPath("$.series[1].points", hasSize(3)))
        .andExpect(jsonPath("$.series[1].points[1].value").value(9710.6));
  }

  @Test
  void timeseries_callsOnlyTheServicesOfTheRequestedMetrics() throws Exception {
    getAsManager(
            TIMESERIES + "?metrics=DISTANCE_KM&from=" + FROM + "&to=" + TO + "&granularity=WEEK")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.granularity").value("WEEK"));

    verify(routesClient).stats(FROM, TO, "WEEK");
    verify(fuelClient, never()).stats(anyString(), anyString(), anyString());
    verify(maintenanceClient, never()).stats(anyString(), anyString(), anyString());
    verify(alertsClient, never()).stats(anyString(), anyString(), anyString());
  }

  @Test
  void timeseries_usesDayAsDefaultGranularity() throws Exception {
    getAsManager(TIMESERIES + "?metrics=ALERTS_CREATED&from=" + FROM + "&to=" + TO)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.series[0].metric").value("ALERTS_CREATED"))
        .andExpect(jsonPath("$.series[0].unit").value("alerts"))
        .andExpect(jsonPath("$.series[0].points[1].value").value(5));

    verify(alertsClient).stats(FROM, TO, DAY);
  }

  @Test
  void timeseries_whenFuelFails_onlyItsMetricsAreLeftOut() throws Exception {
    when(fuelClient.stats(anyString(), anyString(), anyString()))
        .thenThrow(FeignFailures.unavailable());

    getAsManager(
            TIMESERIES
                + "?metrics=DISTANCE_KM,FUEL_COST,FUEL_LITERS,MAINTENANCE_COST&from="
                + FROM
                + "&to="
                + TO)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.partial").value(true))
        .andExpect(jsonPath(UNAVAILABLE, hasSize(1)))
        .andExpect(jsonPath("$.unavailable[0]").value(FUEL))
        .andExpect(jsonPath("$.series", hasSize(2)))
        .andExpect(jsonPath("$.series[0].metric").value("DISTANCE_KM"))
        .andExpect(jsonPath("$.series[1].metric").value("MAINTENANCE_COST"));
  }

  @Test
  void timeseries_ignoresRepeatedMetrics() throws Exception {
    getAsManager(TIMESERIES + "?metrics=DISTANCE_KM,DISTANCE_KM&from=" + FROM + "&to=" + TO)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.series", hasSize(1)));
  }

  @Test
  void timeseries_withRangeOf366Days_isAccepted() throws Exception {
    getAsManager(TIMESERIES + "?metrics=DISTANCE_KM&from=2026-01-01&to=2027-01-01")
        .andExpect(status().isOk());
  }

  @Test
  void timeseries_withRangeOver366Days_isRejected() throws Exception {
    getAsManager(TIMESERIES + "?metrics=DISTANCE_KM&from=2026-01-01&to=2027-01-02")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath(ERROR).value(VALIDATION_ERROR))
        .andExpect(jsonPath("$.details[0].field").value("to"));
  }

  @Test
  void timeseries_withAnEndBeforeTheStart_isRejected() throws Exception {
    getAsManager(TIMESERIES + "?metrics=DISTANCE_KM&from=" + TO + "&to=" + FROM)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath(ERROR).value(VALIDATION_ERROR))
        .andExpect(jsonPath("$.details[0].field").value("to"));
  }

  @Test
  void timeseries_withAnUnknownMetric_isRejected() throws Exception {
    getAsManager(TIMESERIES + "?metrics=DISTANCE_KM,SPEED&from=" + FROM + "&to=" + TO)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath(ERROR).value(VALIDATION_ERROR))
        .andExpect(jsonPath("$.details[0].field").value("metrics"));
  }

  @Test
  void timeseries_withMissingParameter_isRejected() throws Exception {
    getAsManager(TIMESERIES + "?from=" + FROM + "&to=" + TO)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details[0].field").value("metrics"));
    getAsManager(TIMESERIES + "?metrics=DISTANCE_KM&to=" + TO)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details[0].field").value("from"));
    getAsManager(TIMESERIES + "?metrics=DISTANCE_KM&from=" + FROM)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details[0].field").value("to"));
  }

  @Test
  void timeseries_withInvalidDateOrGranularity_isRejected() throws Exception {
    getAsManager(TIMESERIES + "?metrics=DISTANCE_KM&from=2026-13-45&to=" + TO)
        .andExpect(status().isBadRequest());
    getAsManager(
            TIMESERIES + "?metrics=DISTANCE_KM&from=" + FROM + "&to=" + TO + "&granularity=YEAR")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details[0].field").value("granularity"));
  }

  // ----------------------------------------------------------------- ranking

  @Test
  void ranking_byConsumption_sortsFromHighestToLowest() throws Exception {
    getAsManager(RANKING + "?metric=CONSUMPTION")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.metric").value("CONSUMPTION"))
        .andExpect(jsonPath("$.unit").value("L/100 km"))
        .andExpect(jsonPath("$.period.from").exists())
        .andExpect(jsonPath("$.ranking", hasSize(3)))
        .andExpect(jsonPath("$.ranking[0].rank").value(1))
        .andExpect(jsonPath("$.ranking[0].plate").value(SourceData.TRUCK_PLATE))
        .andExpect(jsonPath("$.ranking[0].type").value("TRUCK"))
        .andExpect(jsonPath("$.ranking[0].value").value(27.5))
        .andExpect(jsonPath("$.ranking[1].rank").value(2))
        .andExpect(jsonPath("$.ranking[1].plate").value(SourceData.VAN_PLATE))
        .andExpect(jsonPath("$.ranking[2].rank").value(3))
        .andExpect(jsonPath("$.ranking[2].value").value(7.2));
  }

  @Test
  void ranking_byFuelCost_usesTheCost() throws Exception {
    getAsManager(RANKING + "?metric=FUEL_COST")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.unit").value("EUR"))
        .andExpect(jsonPath("$.ranking[0].plate").value(SourceData.TRUCK_PLATE))
        .andExpect(jsonPath("$.ranking[0].value").value(2951.96))
        .andExpect(jsonPath("$.ranking[1].value").value(1118.29))
        .andExpect(jsonPath("$.ranking[2].value").value(512.4));
  }

  @Test
  void ranking_appliesLimitAndType() throws Exception {
    getAsManager(RANKING + "?metric=FUEL_COST&limit=1")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ranking", hasSize(1)));
    getAsManager(RANKING + "?metric=FUEL_COST&type=VAN")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ranking", hasSize(1)))
        .andExpect(jsonPath("$.ranking[0].plate").value(SourceData.VAN_PLATE));
  }

  @Test
  void ranking_byConsumption_skipsVehiclesWithoutDistance() throws Exception {
    when(fuelClient.consumption(anyString(), anyString()))
        .thenReturn(
            new FuelConsumptionDto(
                List.of(
                    SourceData.consumption(
                        SourceData.VAN_ID, SourceData.VAN_PLATE, "VAN", "100", null),
                    SourceData.consumption(
                        SourceData.CAR_ID, SourceData.CAR_PLATE, "CAR", "50", "7.2"))));

    getAsManager(RANKING + "?metric=CONSUMPTION")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ranking", hasSize(1)))
        .andExpect(jsonPath("$.ranking[0].plate").value(SourceData.CAR_PLATE));
  }

  @Test
  void ranking_whenFuelFails_answers503NamingTheService() throws Exception {
    when(fuelClient.consumption(anyString(), anyString())).thenThrow(FeignFailures.unavailable());

    getAsManager(RANKING + "?metric=CONSUMPTION")
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath(ERROR).value("SERVICE_UNAVAILABLE"))
        .andExpect(jsonPath("$.service").value(FUEL));
  }

  @Test
  void ranking_withInvalidParameters_isRejected() throws Exception {
    getAsManager(RANKING)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details[0].field").value("metric"));
    getAsManager(RANKING + "?metric=SPEED").andExpect(status().isBadRequest());
    getAsManager(RANKING + "?metric=FUEL_COST&limit=0").andExpect(status().isBadRequest());
    getAsManager(RANKING + "?metric=FUEL_COST&limit=21")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details[0].field").value("limit"));
    getAsManager(RANKING + "?metric=FUEL_COST&days=0").andExpect(status().isBadRequest());
    getAsManager(RANKING + "?metric=FUEL_COST&type=BOAT")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details[0].field").value("type"));
  }

  @Test
  void ranking_withTheMaximumLimit_isAccepted() throws Exception {
    getAsManager(RANKING + "?metric=FUEL_COST&limit=20").andExpect(status().isOk());
  }
}
