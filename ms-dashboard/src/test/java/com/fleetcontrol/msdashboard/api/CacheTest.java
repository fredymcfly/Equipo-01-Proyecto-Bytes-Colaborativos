package com.fleetcontrol.msdashboard.api;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fleetcontrol.msdashboard.support.AbstractApiTest;
import com.fleetcontrol.msdashboard.support.FeignFailures;
import com.fleetcontrol.msdashboard.support.SourceData;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

/** Each answer is kept in the cache, keyed by the endpoint and all its parameters. */
class CacheTest extends AbstractApiTest {

  private static final String DASHBOARD = "/api/dashboard";
  private static final String TIMESERIES =
      "/api/dashboard/timeseries?metrics=DISTANCE_KM&from=2026-09-07&to=2026-09-27";
  private static final String RANKING = "/api/dashboard/vehicles/ranking?metric=FUEL_COST";

  private void call(String url) throws Exception {
    mockMvc
        .perform(get(url).header(HttpHeaders.AUTHORIZATION, bearer("MANAGER")))
        .andExpect(status().isOk());
  }

  @Test
  void overview_isServedFromTheCacheOnTheSecondCall() throws Exception {
    call(DASHBOARD);
    call(DASHBOARD);

    verify(vehiclesClient, times(1)).summary();
    verify(routesClient, times(1)).stats(anyString(), anyString(), anyString());
    verify(alertsClient, times(1)).stats(anyString(), anyString(), anyString());
  }

  @Test
  void overview_withDifferentDays_isCachedSeparately() throws Exception {
    call(DASHBOARD + "?days=10");
    call(DASHBOARD + "?days=20");
    call(DASHBOARD + "?days=10");

    verify(vehiclesClient, times(2)).summary();
  }

  @Test
  void overview_whenPartial_isNotCached() throws Exception {
    when(fuelClient.stats(anyString(), anyString(), anyString()))
        .thenThrow(FeignFailures.unavailable())
        .thenReturn(SourceData.fuelStats());

    mockMvc
        .perform(get(DASHBOARD).header(HttpHeaders.AUTHORIZATION, bearer("MANAGER")))
        .andExpect(jsonPath("$.partial").value(true));
    mockMvc
        .perform(get(DASHBOARD).header(HttpHeaders.AUTHORIZATION, bearer("MANAGER")))
        .andExpect(jsonPath("$.partial").value(false))
        .andExpect(jsonPath("$.kpis.fuelCost").value(6846.61));

    verify(fuelClient, times(2)).stats(anyString(), anyString(), anyString());
  }

  @Test
  void timeseries_isCachedPerParameters() throws Exception {
    call(TIMESERIES);
    call(TIMESERIES);
    call(TIMESERIES + "&granularity=WEEK");

    verify(routesClient, times(2)).stats(anyString(), anyString(), anyString());
  }

  @Test
  void ranking_isCachedPerParameters() throws Exception {
    call(RANKING);
    call(RANKING);
    call(RANKING + "&limit=2");

    verify(fuelClient, times(2)).consumption(anyString(), anyString());
  }
}
