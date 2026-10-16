package com.fleetcontrol.msdashboard.service;

import com.fleetcontrol.msdashboard.client.AlertsClient;
import com.fleetcontrol.msdashboard.client.FuelClient;
import com.fleetcontrol.msdashboard.client.MaintenanceClient;
import com.fleetcontrol.msdashboard.client.RoutesClient;
import com.fleetcontrol.msdashboard.constants.BusinessConstants;
import com.fleetcontrol.msdashboard.constants.ConfigConstants;
import com.fleetcontrol.msdashboard.models.dto.internal.AlertsStatsDto;
import com.fleetcontrol.msdashboard.models.dto.internal.FuelStatsDto;
import com.fleetcontrol.msdashboard.models.dto.internal.MaintenanceStatsDto;
import com.fleetcontrol.msdashboard.models.dto.internal.RoutesStatsDto;
import com.fleetcontrol.msdashboard.models.dto.request.TimeseriesQuery;
import com.fleetcontrol.msdashboard.models.dto.response.MetricSeriesResponse;
import com.fleetcontrol.msdashboard.models.dto.response.SeriesPointResponse;
import com.fleetcontrol.msdashboard.models.dto.response.TimeseriesResponse;
import com.fleetcontrol.msdashboard.models.enums.DashboardMetric;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Builds the time series. It only calls the services that own the requested metrics, all at the
 * same time. If one of them does not answer, only its metrics are left out.
 */
@Service
@RequiredArgsConstructor
public class TimeseriesService {

  private final RoutesClient routesClient;
  private final FuelClient fuelClient;
  private final MaintenanceClient maintenanceClient;
  private final AlertsClient alertsClient;
  private final SourceCaller caller;

  /** One series for each requested metric. */
  @Cacheable(
      cacheNames = BusinessConstants.Cache.TIMESERIES,
      key = "#query",
      unless = "#result.partial()")
  public TimeseriesResponse timeseries(TimeseriesQuery query) {
    Map<String, CompletableFuture<SourceResult<SeriesSet>>> sources = startCalls(query);
    List<MetricSeriesResponse> series = new ArrayList<>();
    Set<String> unavailable = new LinkedHashSet<>();
    for (DashboardMetric metric : query.metrics()) {
      SourceResult<SeriesSet> result = sources.get(metric.getService()).join();
      if (result.isAvailable()) {
        series.add(
            new MetricSeriesResponse(
                metric, metric.getUnit(), result.data().byMetric().get(metric)));
      } else {
        unavailable.add(result.service());
      }
    }
    return new TimeseriesResponse(
        query.from(),
        query.to(),
        query.granularity(),
        !unavailable.isEmpty(),
        List.copyOf(unavailable),
        series);
  }

  /** Starts, at the same time, the calls to the services that own the requested metrics. */
  private Map<String, CompletableFuture<SourceResult<SeriesSet>>> startCalls(
      TimeseriesQuery query) {
    String from = query.from().toString();
    String to = query.to().toString();
    String granularity = query.granularity().name();
    Map<String, CompletableFuture<SourceResult<SeriesSet>>> sources = new HashMap<>();
    sources.put(
        ConfigConstants.Services.ROUTES,
        start(
            query,
            ConfigConstants.Services.ROUTES,
            () -> routesSeries(routesClient.stats(from, to, granularity))));
    sources.put(
        ConfigConstants.Services.FUEL,
        start(
            query,
            ConfigConstants.Services.FUEL,
            () -> fuelSeries(fuelClient.stats(from, to, granularity))));
    sources.put(
        ConfigConstants.Services.MAINTENANCE,
        start(
            query,
            ConfigConstants.Services.MAINTENANCE,
            () -> maintenanceSeries(maintenanceClient.stats(from, to, granularity))));
    sources.put(
        ConfigConstants.Services.ALERTS,
        start(
            query,
            ConfigConstants.Services.ALERTS,
            () -> alertsSeries(alertsClient.stats(from, to, granularity))));
    return sources;
  }

  /** Starts the call only when a requested metric needs the service. */
  private CompletableFuture<SourceResult<SeriesSet>> start(
      TimeseriesQuery query, String service, Supplier<SeriesSet> request) {
    if (query.requires(service)) {
      return caller.call(service, request);
    }
    return CompletableFuture.completedFuture(new SourceResult<>(service, null));
  }

  private SeriesSet routesSeries(RoutesStatsDto stats) {
    return new SeriesSet(
        Map.of(
            DashboardMetric.DISTANCE_KM,
            points(
                stats.series(), RoutesStatsDto.Point::period, RoutesStatsDto.Point::distanceKm)));
  }

  private SeriesSet fuelSeries(FuelStatsDto stats) {
    return new SeriesSet(
        Map.of(
            DashboardMetric.FUEL_LITERS,
            points(stats.series(), FuelStatsDto.Point::period, FuelStatsDto.Point::liters),
            DashboardMetric.FUEL_COST,
            points(stats.series(), FuelStatsDto.Point::period, FuelStatsDto.Point::cost),
            DashboardMetric.CONSUMPTION_L100KM,
            points(stats.series(), FuelStatsDto.Point::period, FuelStatsDto.Point::avgL100km)));
  }

  private SeriesSet maintenanceSeries(MaintenanceStatsDto stats) {
    return new SeriesSet(
        Map.of(
            DashboardMetric.MAINTENANCE_COST,
            points(
                stats.series(),
                MaintenanceStatsDto.Point::period,
                MaintenanceStatsDto.Point::cost)));
  }

  private SeriesSet alertsSeries(AlertsStatsDto stats) {
    return new SeriesSet(
        Map.of(
            DashboardMetric.ALERTS_CREATED,
            points(
                stats.series(),
                AlertsStatsDto.Point::period,
                point -> BigDecimal.valueOf(point.created()))));
  }

  private <P> List<SeriesPointResponse> points(
      List<P> source, Function<P, String> period, Function<P, BigDecimal> value) {
    List<P> items = source == null ? List.of() : source;
    return items.stream()
        .map(item -> new SeriesPointResponse(period.apply(item), value.apply(item)))
        .toList();
  }

  /** Points of every metric that one service provides. */
  private record SeriesSet(Map<DashboardMetric, List<SeriesPointResponse>> byMetric) {}
}
