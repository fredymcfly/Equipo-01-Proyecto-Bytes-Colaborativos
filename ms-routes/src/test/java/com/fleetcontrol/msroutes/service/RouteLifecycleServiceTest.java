package com.fleetcontrol.msroutes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fleetcontrol.msroutes.client.DriverClient;
import com.fleetcontrol.msroutes.client.VehicleClient;
import com.fleetcontrol.msroutes.client.dto.VehicleResponse;
import com.fleetcontrol.msroutes.client.dto.VehicleStatusRequest;
import com.fleetcontrol.msroutes.client.enums.VehicleStatus;
import com.fleetcontrol.msroutes.client.enums.VehicleType;
import com.fleetcontrol.msroutes.dto.CompleteRouteRequest;
import com.fleetcontrol.msroutes.dto.RouteResponse;
import com.fleetcontrol.msroutes.dto.RouteStatsResponse;
import com.fleetcontrol.msroutes.entities.Route;
import com.fleetcontrol.msroutes.enums.Granularity;
import com.fleetcontrol.msroutes.enums.RouteStatus;
import com.fleetcontrol.msroutes.exception.ApiException;
import com.fleetcontrol.msroutes.exception.ServiceUnavailableException;
import com.fleetcontrol.msroutes.repository.RouteRepository;
import com.fleetcontrol.msroutes.repository.RouteStatsRow;
import feign.FeignException;
import feign.Request;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;
import org.springframework.http.HttpStatus;

/** Unit tests for starting and completing routes and for the statistics of {@link RouteService}. */
@ExtendWith(MockitoExtension.class)
class RouteLifecycleServiceTest {

  private static final UUID VEHICLE_ID = UUID.fromString("6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01");
  private static final UUID DRIVER_ID = UUID.fromString("3b2d8f10-7c4a-4e55-b1a0-5d9e2c7f1a01");
  private static final UUID ROUTE_ID = UUID.fromString("c8e1b7a4-52d9-4f06-8e3b-1a7d90c4f201");
  private static final Instant NOW = Instant.parse("2026-10-06T11:20:45Z");
  private static final int ODOMETER = 45210;

  @Mock private RouteRepository routeRepository;
  @Mock private VehicleClient vehicleClient;
  @Mock private DriverClient driverClient;

  private RouteService routeService;

  @BeforeEach
  void setUp() {
    routeService =
        new RouteService(
            routeRepository, vehicleClient, driverClient, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  // ---------- startRoute ----------

  @Test
  void startRoute_plannedRouteAndAvailableVehicle_startsIt() {
    stubRoute(RouteStatus.PLANNED);
    stubVehicle(VehicleStatus.AVAILABLE);
    when(routeRepository.save(any(Route.class))).thenAnswer(inv -> inv.getArgument(0));

    RouteResponse response = routeService.startRoute(ROUTE_ID);

    verify(vehicleClient)
        .updateStatus(VEHICLE_ID, new VehicleStatusRequest(VehicleStatus.IN_USE, null));
    assertThat(response.status()).isEqualTo(RouteStatus.IN_PROGRESS);
    assertThat(response.startedAt()).isEqualTo(NOW);
    assertThat(response.startOdometerKm()).isEqualTo(ODOMETER);
  }

  @Test
  void startRoute_unknownRoute_failsWithNotFound() {
    when(routeRepository.findById(ROUTE_ID)).thenReturn(Optional.empty());

    assertApiError(
        () -> routeService.startRoute(ROUTE_ID), HttpStatus.NOT_FOUND, "ROUTE_NOT_FOUND");
  }

  @Test
  void startRoute_routeNotPlanned_failsBeforeCallingTheVehicle() {
    stubRoute(RouteStatus.IN_PROGRESS);

    assertApiError(
        () -> routeService.startRoute(ROUTE_ID), HttpStatus.CONFLICT, "INVALID_ROUTE_STATE");
    verify(vehicleClient, never()).getVehicle(any());
  }

  @Test
  void startRoute_vehicleNotAvailable_failsWithoutChangingIt() {
    stubRoute(RouteStatus.PLANNED);
    stubVehicle(VehicleStatus.IN_USE);

    assertApiError(
        () -> routeService.startRoute(ROUTE_ID), HttpStatus.CONFLICT, "VEHICLE_NOT_AVAILABLE");
    verify(vehicleClient, never()).updateStatus(any(), any());
    verify(routeRepository, never()).save(any());
  }

  @Test
  void startRoute_vehicleServiceDown_failsWithServiceUnavailable() {
    stubRoute(RouteStatus.PLANNED);
    when(vehicleClient.getVehicle(VEHICLE_ID)).thenThrow(serverError());

    assertThatThrownBy(() -> routeService.startRoute(ROUTE_ID))
        .isInstanceOf(ServiceUnavailableException.class);
  }

  @Test
  void startRoute_statusChangeFails_failsAndDoesNotSave() {
    stubRoute(RouteStatus.PLANNED);
    stubVehicle(VehicleStatus.AVAILABLE);
    when(vehicleClient.updateStatus(any(), any())).thenThrow(serverError());

    assertThatThrownBy(() -> routeService.startRoute(ROUTE_ID))
        .isInstanceOf(ServiceUnavailableException.class);
    verify(routeRepository, never()).save(any());
  }

  @Test
  void startRoute_saveFails_releasesTheVehicle() {
    stubRoute(RouteStatus.PLANNED);
    stubVehicle(VehicleStatus.AVAILABLE);
    when(routeRepository.save(any(Route.class))).thenThrow(new IllegalStateException("db down"));

    assertThatThrownBy(() -> routeService.startRoute(ROUTE_ID))
        .isInstanceOf(IllegalStateException.class);

    ArgumentCaptor<VehicleStatusRequest> sent = ArgumentCaptor.forClass(VehicleStatusRequest.class);
    verify(vehicleClient, times(2)).updateStatus(any(), sent.capture());
    assertThat(sent.getAllValues().get(1))
        .isEqualTo(new VehicleStatusRequest(VehicleStatus.AVAILABLE, null));
  }

  @Test
  void startRoute_saveAndReleaseFail_keepsTheOriginalError() {
    stubRoute(RouteStatus.PLANNED);
    stubVehicle(VehicleStatus.AVAILABLE);
    when(routeRepository.save(any(Route.class))).thenThrow(new IllegalStateException("db down"));
    when(vehicleClient.updateStatus(any(), any())).thenReturn(null).thenThrow(serverError());

    assertThatThrownBy(() -> routeService.startRoute(ROUTE_ID))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("db down");
  }

  @Test
  void startRoute_vehicleCircuitOpen_failsWithServiceUnavailable() {
    stubRoute(RouteStatus.PLANNED);
    when(vehicleClient.getVehicle(VEHICLE_ID)).thenThrow(circuitBroken());

    assertThatThrownBy(() -> routeService.startRoute(ROUTE_ID))
        .isInstanceOf(ServiceUnavailableException.class);
  }

  @Test
  void startRoute_saveFailsAndTheCircuitIsOpen_keepsTheOriginalError() {
    stubRoute(RouteStatus.PLANNED);
    stubVehicle(VehicleStatus.AVAILABLE);
    when(routeRepository.save(any(Route.class))).thenThrow(new IllegalStateException("db down"));
    when(vehicleClient.updateStatus(any(), any())).thenReturn(null).thenThrow(circuitBroken());

    assertThatThrownBy(() -> routeService.startRoute(ROUTE_ID))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("db down");
  }

  @Test
  void completeRoute_circuitOpen_keepsTheRouteInProgress() {
    stubRoute(RouteStatus.IN_PROGRESS);
    stubVehicle(VehicleStatus.IN_USE);
    when(vehicleClient.updateStatus(any(), any())).thenThrow(circuitBroken());

    assertThatThrownBy(
            () ->
                routeService.completeRoute(
                    ROUTE_ID, new CompleteRouteRequest(BigDecimal.TEN, null)))
        .isInstanceOf(ServiceUnavailableException.class);
    verify(routeRepository, never()).save(any());
  }

  // ---------- completeRoute ----------

  @Test
  void completeRoute_routeInProgress_roundsTheDistanceAndReleasesTheVehicle() {
    stubRoute(RouteStatus.IN_PROGRESS);
    stubVehicle(VehicleStatus.IN_USE);
    when(routeRepository.save(any(Route.class))).thenAnswer(inv -> inv.getArgument(0));

    RouteResponse response =
        routeService.completeRoute(
            ROUTE_ID, new CompleteRouteRequest(new BigDecimal("362.4"), "Sin incidencias"));

    verify(vehicleClient)
        .updateStatus(
            VEHICLE_ID, new VehicleStatusRequest(VehicleStatus.AVAILABLE, ODOMETER + 362));
    assertThat(response.status()).isEqualTo(RouteStatus.COMPLETED);
    assertThat(response.endedAt()).isEqualTo(NOW);
    assertThat(response.endOdometerKm()).isEqualTo(ODOMETER + 362);
    assertThat(response.actualDistanceKm()).isEqualByComparingTo("362.4");
    assertThat(response.notes()).isEqualTo("Sin incidencias");
  }

  @Test
  void completeRoute_halfKilometre_roundsUp() {
    stubRoute(RouteStatus.IN_PROGRESS);
    stubVehicle(VehicleStatus.IN_USE);
    when(routeRepository.save(any(Route.class))).thenAnswer(inv -> inv.getArgument(0));

    RouteResponse response =
        routeService.completeRoute(
            ROUTE_ID, new CompleteRouteRequest(new BigDecimal("10.5"), null));

    assertThat(response.endOdometerKm()).isEqualTo(ODOMETER + 11);
  }

  @Test
  void completeRoute_routeNotInProgress_failsWithInvalidState() {
    stubRoute(RouteStatus.PLANNED);

    assertApiError(
        () -> routeService.completeRoute(ROUTE_ID, new CompleteRouteRequest(BigDecimal.ONE, null)),
        HttpStatus.CONFLICT,
        "INVALID_ROUTE_STATE");
  }

  @Test
  void completeRoute_vehicleServiceDown_keepsTheRouteInProgress() {
    stubRoute(RouteStatus.IN_PROGRESS);
    stubVehicle(VehicleStatus.IN_USE);
    when(vehicleClient.updateStatus(any(), any())).thenThrow(serverError());

    assertThatThrownBy(
            () ->
                routeService.completeRoute(
                    ROUTE_ID, new CompleteRouteRequest(BigDecimal.TEN, null)))
        .isInstanceOf(ServiceUnavailableException.class);
    verify(routeRepository, never()).save(any());
  }

  @Test
  void completeRoute_retryAfterTheVehicleWasReleased_doesNotChangeItAgain() {
    stubRoute(RouteStatus.IN_PROGRESS);
    when(vehicleClient.getVehicle(VEHICLE_ID))
        .thenReturn(
            new VehicleResponse(
                VEHICLE_ID, VehicleType.VAN, VehicleStatus.AVAILABLE, ODOMETER + 10));
    when(routeRepository.save(any(Route.class))).thenAnswer(inv -> inv.getArgument(0));

    RouteResponse response =
        routeService.completeRoute(ROUTE_ID, new CompleteRouteRequest(BigDecimal.TEN, null));

    verify(vehicleClient, never()).updateStatus(any(), any());
    assertThat(response.status()).isEqualTo(RouteStatus.COMPLETED);
  }

  // ---------- stats ----------

  @Test
  void stats_groupsTheRowsAndAddsTheTotals() {
    when(routeRepository.completedStats(
            "week",
            Instant.parse("2026-09-07T00:00:00Z"),
            Instant.parse("2026-09-28T00:00:00Z"),
            null))
        .thenReturn(
            List.of(
                row("2026-09-07", 97L, "9034.14", 128.4),
                row("2026-09-14", 104L, "9710.56", 130.6)));

    RouteStatsResponse stats =
        routeService.stats(
            LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 27), Granularity.WEEK, null);

    assertThat(stats.granularity()).isEqualTo(Granularity.WEEK);
    assertThat(stats.totals().routes()).isEqualTo(201);
    assertThat(stats.totals().distanceKm()).isEqualByComparingTo("18744.7");
    assertThat(stats.series()).hasSize(2);
    assertThat(stats.series().get(0).distanceKm()).isEqualByComparingTo("9034.1");
    assertThat(stats.series().get(0).avgDurationMin()).isEqualTo(128);
    assertThat(stats.series().get(1).avgDurationMin()).isEqualTo(131);
  }

  @Test
  void stats_withoutGranularityAndWithVehicle_usesDayAndFiltersByVehicle() {
    when(routeRepository.completedStats(any(), any(), any(), any())).thenReturn(List.of());

    RouteStatsResponse stats =
        routeService.stats(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 1), null, VEHICLE_ID);

    verify(routeRepository)
        .completedStats(
            "day",
            Instant.parse("2026-09-01T00:00:00Z"),
            Instant.parse("2026-09-02T00:00:00Z"),
            VEHICLE_ID.toString());
    assertThat(stats.granularity()).isEqualTo(Granularity.DAY);
    assertThat(stats.totals().routes()).isZero();
    assertThat(stats.totals().distanceKm()).isEqualByComparingTo("0");
  }

  @Test
  void stats_fromAfterTo_failsWithValidationError() {
    assertApiError(
        () ->
            routeService.stats(
                LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 1), Granularity.DAY, null),
        HttpStatus.BAD_REQUEST,
        "VALIDATION_ERROR");
  }

  // ---------- helpers ----------

  private void stubRoute(RouteStatus status) {
    Route route =
        Route.builder()
            .id(ROUTE_ID)
            .vehicleId(VEHICLE_ID)
            .driverId(DRIVER_ID)
            .origin("Madrid - Centro Logístico")
            .destination("Valencia - Puerto")
            .plannedStart(Instant.parse("2026-10-06T07:00:00Z"))
            .estimatedDurationMin(240)
            .plannedDistanceKm(new BigDecimal("355.0"))
            .status(status)
            .startOdometerKm(status == RouteStatus.IN_PROGRESS ? ODOMETER : null)
            .build();
    when(routeRepository.findById(ROUTE_ID)).thenReturn(Optional.of(route));
  }

  private void stubVehicle(VehicleStatus status) {
    when(vehicleClient.getVehicle(VEHICLE_ID))
        .thenReturn(new VehicleResponse(VEHICLE_ID, VehicleType.VAN, status, ODOMETER));
  }

  private static RouteStatsRow row(String period, long routes, String distance, double duration) {
    return new RouteStatsRow() {
      @Override
      public String getPeriod() {
        return period;
      }

      @Override
      public Long getRoutes() {
        return routes;
      }

      @Override
      public BigDecimal getDistanceKm() {
        return new BigDecimal(distance);
      }

      @Override
      public Double getAvgDurationMin() {
        return duration;
      }
    };
  }

  private static NoFallbackAvailableException circuitBroken() {
    return new NoFallbackAvailableException("No fallback available.", new TimeoutException());
  }

  private static FeignException serverError() {
    Request request =
        Request.create(
            Request.HttpMethod.PATCH,
            "http://ms-vehicles/api",
            Map.of(),
            (byte[]) null,
            StandardCharsets.UTF_8,
            null);
    return new FeignException.ServiceUnavailable("unavailable", request, new byte[0], Map.of());
  }

  private static void assertApiError(ThrowingCallable call, HttpStatus status, String code) {
    assertThatThrownBy(call)
        .isInstanceOfSatisfying(
            ApiException.class,
            e -> {
              assertThat(e.getStatus()).isEqualTo(status);
              assertThat(e.getCode()).isEqualTo(code);
            });
  }
}
