package com.fleetcontrol.msdashboard.service;

import com.fleetcontrol.msdashboard.client.AlertsClient;
import com.fleetcontrol.msdashboard.client.FuelClient;
import com.fleetcontrol.msdashboard.client.MaintenanceClient;
import com.fleetcontrol.msdashboard.client.RoutesClient;
import com.fleetcontrol.msdashboard.client.VehiclesClient;
import com.fleetcontrol.msdashboard.constants.BusinessConstants;
import com.fleetcontrol.msdashboard.constants.ConfigConstants;
import com.fleetcontrol.msdashboard.models.dto.internal.AlertsStatsDto;
import com.fleetcontrol.msdashboard.models.dto.internal.FleetSummaryDto;
import com.fleetcontrol.msdashboard.models.dto.internal.FuelStatsDto;
import com.fleetcontrol.msdashboard.models.dto.internal.MaintenanceOrdersDto;
import com.fleetcontrol.msdashboard.models.dto.internal.MaintenanceStatsDto;
import com.fleetcontrol.msdashboard.models.dto.internal.RoutesStatsDto;
import com.fleetcontrol.msdashboard.models.dto.internal.VehicleDto;
import com.fleetcontrol.msdashboard.models.dto.request.DashboardQuery;
import com.fleetcontrol.msdashboard.models.dto.response.AlertsSummaryResponse;
import com.fleetcontrol.msdashboard.models.dto.response.DashboardResponse;
import com.fleetcontrol.msdashboard.models.dto.response.FleetStatusResponse;
import com.fleetcontrol.msdashboard.models.dto.response.KpisResponse;
import com.fleetcontrol.msdashboard.models.dto.response.PeriodResponse;
import com.fleetcontrol.msdashboard.models.dto.response.UpcomingMaintenanceResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Builds the executive summary of the fleet. Every source service is called at the same time. The
 * answer is kept in the cache, except when it is partial, so that a service that comes back is used
 * right away.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

  private final VehiclesClient vehiclesClient;
  private final RoutesClient routesClient;
  private final FuelClient fuelClient;
  private final MaintenanceClient maintenanceClient;
  private final AlertsClient alertsClient;
  private final SourceCaller caller;
  private final Clock clock;

  /** Summary of the last {@code days} days: fleet status, indicators, alerts and maintenance. */
  @Cacheable(
      cacheNames = BusinessConstants.Cache.OVERVIEW,
      key = "#query",
      unless = "#result.partial()")
  public DashboardResponse overview(DashboardQuery query) {
    LocalDate to = LocalDate.now(clock);
    LocalDate from = to.minusDays(query.days());
    Sources sources = fetch(from.toString(), to.toString());
    Map<UUID, String> plates = plates(sources);
    return new DashboardResponse(
        Instant.now(clock),
        new PeriodResponse(from, to),
        !sources.unavailable().isEmpty(),
        sources.unavailable(),
        extract(sources.fleet(), this::toFleet),
        toKpis(sources),
        extract(sources.alerts(), this::toAlerts),
        extract(sources.orders(), orders -> toUpcoming(orders, plates)));
  }

  private Sources fetch(String from, String to) {
    final CompletableFuture<SourceResult<FleetSummaryDto>> fleet =
        caller.call(ConfigConstants.Services.VEHICLES, vehiclesClient::summary);
    final CompletableFuture<SourceResult<RoutesStatsDto>> routes =
        caller.call(
            ConfigConstants.Services.ROUTES,
            () -> routesClient.stats(from, to, BusinessConstants.Overview.KPI_GRANULARITY));
    final CompletableFuture<SourceResult<FuelStatsDto>> fuel =
        caller.call(
            ConfigConstants.Services.FUEL,
            () -> fuelClient.stats(from, to, BusinessConstants.Overview.KPI_GRANULARITY));
    final CompletableFuture<SourceResult<MaintenanceStatsDto>> maintenance =
        caller.call(
            ConfigConstants.Services.MAINTENANCE,
            () -> maintenanceClient.stats(from, to, BusinessConstants.Overview.KPI_GRANULARITY));
    final CompletableFuture<SourceResult<MaintenanceOrdersDto>> orders =
        caller.call(
            ConfigConstants.Services.MAINTENANCE,
            () ->
                maintenanceClient.orders(
                    BusinessConstants.Overview.PENDING_STATUS,
                    BusinessConstants.Overview.FIRST_PAGE,
                    BusinessConstants.Overview.UPCOMING_LIMIT));
    final CompletableFuture<SourceResult<AlertsStatsDto>> alerts =
        caller.call(
            ConfigConstants.Services.ALERTS,
            () -> alertsClient.stats(from, to, BusinessConstants.Overview.KPI_GRANULARITY));
    return new Sources(
        fleet.join(), routes.join(), fuel.join(), maintenance.join(), orders.join(), alerts.join());
  }

  /** Looks up the plate of each vehicle with a pending order, all at the same time. */
  private Map<UUID, String> plates(Sources sources) {
    if (!sources.fleet().isAvailable() || !sources.orders().isAvailable()) {
      return Map.of();
    }
    Map<UUID, CompletableFuture<SourceResult<VehicleDto>>> lookups = new HashMap<>();
    for (MaintenanceOrdersDto.Order order : orderList(sources.orders().data())) {
      UUID vehicleId = order.vehicleId();
      lookups.computeIfAbsent(
          vehicleId,
          id ->
              caller.call(ConfigConstants.Services.VEHICLES, () -> vehiclesClient.getVehicle(id)));
    }
    Map<UUID, String> plates = new HashMap<>();
    lookups.forEach(
        (vehicleId, lookup) -> {
          SourceResult<VehicleDto> result = lookup.join();
          if (result.isAvailable()) {
            plates.put(vehicleId, result.data().plate());
          }
        });
    return plates;
  }

  private FleetStatusResponse toFleet(FleetSummaryDto summary) {
    Map<String, Long> byStatus = summary.byStatus() == null ? Map.of() : summary.byStatus();
    return new FleetStatusResponse(
        summary.total(),
        byStatus.getOrDefault(BusinessConstants.VehicleStatuses.AVAILABLE, 0L),
        byStatus.getOrDefault(BusinessConstants.VehicleStatuses.IN_USE, 0L),
        byStatus.getOrDefault(BusinessConstants.VehicleStatuses.IN_MAINTENANCE, 0L),
        byStatus.getOrDefault(BusinessConstants.VehicleStatuses.OUT_OF_SERVICE, 0L));
  }

  private KpisResponse toKpis(Sources sources) {
    return new KpisResponse(
        extract(sources.routes(), stats -> stats.totals().routes()),
        extract(sources.routes(), stats -> stats.totals().distanceKm()),
        extract(sources.fuel(), stats -> stats.totals().liters()),
        extract(sources.fuel(), stats -> stats.totals().cost()),
        extract(sources.fuel(), stats -> stats.totals().avgL100km()),
        extract(sources.maintenance(), stats -> stats.totals().cost()));
  }

  private AlertsSummaryResponse toAlerts(AlertsStatsDto stats) {
    Map<String, Long> open = stats.open() == null ? Map.of() : stats.open();
    long total = open.values().stream().mapToLong(Long::longValue).sum();
    return new AlertsSummaryResponse(
        total,
        open.getOrDefault(BusinessConstants.Severities.CRITICAL, 0L),
        open.getOrDefault(BusinessConstants.Severities.WARNING, 0L),
        open.getOrDefault(BusinessConstants.Severities.INFO, 0L));
  }

  private List<UpcomingMaintenanceResponse> toUpcoming(
      MaintenanceOrdersDto orders, Map<UUID, String> plates) {
    return orderList(orders).stream()
        .map(
            order ->
                new UpcomingMaintenanceResponse(
                    order.id(),
                    order.vehicleId(),
                    plates.get(order.vehicleId()),
                    order.type(),
                    order.scheduledFor()))
        .toList();
  }

  private List<MaintenanceOrdersDto.Order> orderList(MaintenanceOrdersDto orders) {
    return orders.content() == null ? List.of() : orders.content();
  }

  /** Value taken from the answer of a service, or {@code null} when the service is down. */
  private <T, R> R extract(SourceResult<T> result, Function<T, R> getter) {
    return result.isAvailable() ? getter.apply(result.data()) : null;
  }

  /** Answers of the source services, in the order in which they are reported as unavailable. */
  private record Sources(
      SourceResult<FleetSummaryDto> fleet,
      SourceResult<RoutesStatsDto> routes,
      SourceResult<FuelStatsDto> fuel,
      SourceResult<MaintenanceStatsDto> maintenance,
      SourceResult<MaintenanceOrdersDto> orders,
      SourceResult<AlertsStatsDto> alerts) {

    /** Names of the services that did not answer, without repeating any. */
    List<String> unavailable() {
      return Stream.<SourceResult<?>>of(fleet, routes, fuel, maintenance, orders, alerts)
          .filter(result -> !result.isAvailable())
          .map(SourceResult::service)
          .distinct()
          .toList();
    }
  }
}
