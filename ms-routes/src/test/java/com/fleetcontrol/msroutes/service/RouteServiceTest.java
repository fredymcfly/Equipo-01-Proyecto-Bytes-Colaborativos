package com.fleetcontrol.msroutes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fleetcontrol.msroutes.client.DriverClient;
import com.fleetcontrol.msroutes.client.VehicleClient;
import com.fleetcontrol.msroutes.client.dto.DriverResponse;
import com.fleetcontrol.msroutes.client.dto.VehicleResponse;
import com.fleetcontrol.msroutes.client.enums.DriverStatus;
import com.fleetcontrol.msroutes.client.enums.LicenseCategory;
import com.fleetcontrol.msroutes.client.enums.VehicleStatus;
import com.fleetcontrol.msroutes.client.enums.VehicleType;
import com.fleetcontrol.msroutes.dto.CreateRouteRequest;
import com.fleetcontrol.msroutes.dto.PageResponse;
import com.fleetcontrol.msroutes.dto.RouteResponse;
import com.fleetcontrol.msroutes.entities.Route;
import com.fleetcontrol.msroutes.enums.RouteStatus;
import com.fleetcontrol.msroutes.exception.ApiException;
import com.fleetcontrol.msroutes.exception.ServiceUnavailableException;
import com.fleetcontrol.msroutes.repository.RouteRepository;
import feign.FeignException;
import feign.Request;
import feign.RetryableException;
import java.math.BigDecimal;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

/** Unit tests for the business rules of {@link RouteService}. */
@ExtendWith(MockitoExtension.class)
class RouteServiceTest {

  private static final UUID VEHICLE_ID = UUID.fromString("6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01");
  private static final UUID DRIVER_ID = UUID.fromString("3b2d8f10-7c4a-4e55-b1a0-5d9e2c7f1a01");
  private static final UUID USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
  private static final UUID ROUTE_ID = UUID.fromString("c8e1b7a4-52d9-4f06-8e3b-1a7d90c4f201");

  private static final Instant START = Instant.parse("2026-10-06T07:00:00Z");
  private static final int DURATION_MIN = 240;
  private static final Instant END = START.plus(Duration.ofMinutes(DURATION_MIN));
  private static final LocalDate VALID_LICENSE = LocalDate.of(2027, 3, 14);

  @Mock private RouteRepository routeRepository;
  @Mock private VehicleClient vehicleClient;
  @Mock private DriverClient driverClient;

  @InjectMocks private RouteService routeService;

  // ---------- planRoute: happy path ----------

  @Test
  void planRoute_validData_savesPlannedRoute() {
    stubVehicle(VehicleType.VAN, VehicleStatus.AVAILABLE);
    stubDriver(LicenseCategory.B, VALID_LICENSE, DriverStatus.ACTIVE);
    stubNoOverlap();
    when(routeRepository.save(any(Route.class))).thenAnswer(inv -> inv.getArgument(0));

    RouteResponse response = routeService.planRoute(request(), USER_ID);

    ArgumentCaptor<Route> saved = ArgumentCaptor.forClass(Route.class);
    verify(routeRepository).save(saved.capture());
    Route route = saved.getValue();
    assertThat(route.getStatus()).isEqualTo(RouteStatus.PLANNED);
    assertThat(route.getVehicleId()).isEqualTo(VEHICLE_ID);
    assertThat(route.getDriverId()).isEqualTo(DRIVER_ID);
    assertThat(route.getOrigin()).isEqualTo("Madrid - Centro Logístico");
    assertThat(route.getDestination()).isEqualTo("Valencia - Puerto");
    assertThat(route.getPlannedStart()).isEqualTo(START);
    assertThat(route.getEstimatedDurationMin()).isEqualTo(DURATION_MIN);
    assertThat(route.getPlannedDistanceKm()).isEqualByComparingTo("355.0");
    assertThat(route.getCreatedBy()).isEqualTo(USER_ID);
    assertThat(response.status()).isEqualTo(RouteStatus.PLANNED);
  }

  @ParameterizedTest
  @EnumSource(value = VehicleStatus.class, names = "OUT_OF_SERVICE", mode = EnumSource.Mode.EXCLUDE)
  void planRoute_vehicleNotOutOfService_isAccepted(VehicleStatus status) {
    stubVehicle(VehicleType.VAN, status);
    stubDriver(LicenseCategory.B, VALID_LICENSE, DriverStatus.ACTIVE);
    stubNoOverlap();
    when(routeRepository.save(any(Route.class))).thenAnswer(inv -> inv.getArgument(0));

    RouteResponse response = routeService.planRoute(request(), USER_ID);

    assertThat(response.status()).isEqualTo(RouteStatus.PLANNED);
  }

  @Test
  void planRoute_licenseExpiresOnStartDate_isAccepted() {
    stubVehicle(VehicleType.VAN, VehicleStatus.AVAILABLE);
    stubDriver(LicenseCategory.B, LocalDate.of(2026, 10, 6), DriverStatus.ACTIVE);
    stubNoOverlap();
    when(routeRepository.save(any(Route.class))).thenAnswer(inv -> inv.getArgument(0));

    RouteResponse response = routeService.planRoute(request(), USER_ID);

    assertThat(response.status()).isEqualTo(RouteStatus.PLANNED);
  }

  // ---------- planRoute: 409 VEHICLE_NOT_AVAILABLE ----------

  @Test
  void planRoute_vehicleOutOfService_throwsVehicleNotAvailable() {
    stubVehicle(VehicleType.VAN, VehicleStatus.OUT_OF_SERVICE);
    stubDriver(LicenseCategory.B, VALID_LICENSE, DriverStatus.ACTIVE);

    assertApiError(
        () -> routeService.planRoute(request(), USER_ID),
        HttpStatus.CONFLICT,
        "VEHICLE_NOT_AVAILABLE");
    verify(routeRepository, never()).save(any(Route.class));
  }

  // ---------- planRoute: 409 DRIVER_NOT_ELIGIBLE ----------

  @ParameterizedTest
  @EnumSource(value = DriverStatus.class, names = "ACTIVE", mode = EnumSource.Mode.EXCLUDE)
  void planRoute_driverNotActive_throwsDriverNotEligible(DriverStatus status) {
    stubVehicle(VehicleType.VAN, VehicleStatus.AVAILABLE);
    stubDriver(LicenseCategory.B, VALID_LICENSE, status);

    assertApiError(
        () -> routeService.planRoute(request(), USER_ID),
        HttpStatus.CONFLICT,
        "DRIVER_NOT_ELIGIBLE");
    verify(routeRepository, never()).save(any(Route.class));
  }

  @Test
  void planRoute_licenseExpiredBeforeStartDate_throwsDriverNotEligible() {
    stubVehicle(VehicleType.VAN, VehicleStatus.AVAILABLE);
    stubDriver(LicenseCategory.B, LocalDate.of(2026, 10, 5), DriverStatus.ACTIVE);

    assertApiError(
        () -> routeService.planRoute(request(), USER_ID),
        HttpStatus.CONFLICT,
        "DRIVER_NOT_ELIGIBLE");
    verify(routeRepository, never()).save(any(Route.class));
  }

  @ParameterizedTest
  @CsvSource({"A, CAR", "A, VAN", "A, TRUCK", "B, TRUCK", "B, MOTORCYCLE", "C, MOTORCYCLE"})
  void planRoute_incompatibleLicenseCategory_throwsDriverNotEligible(
      LicenseCategory category, VehicleType type) {
    stubVehicle(type, VehicleStatus.AVAILABLE);
    stubDriver(category, VALID_LICENSE, DriverStatus.ACTIVE);

    assertApiError(
        () -> routeService.planRoute(request(), USER_ID),
        HttpStatus.CONFLICT,
        "DRIVER_NOT_ELIGIBLE");
    verify(routeRepository, never()).save(any(Route.class));
  }

  // ---------- planRoute: 409 ROUTE_OVERLAP ----------

  @Test
  void planRoute_vehicleOverlap_throwsRouteOverlap() {
    stubVehicle(VehicleType.VAN, VehicleStatus.AVAILABLE);
    stubDriver(LicenseCategory.B, VALID_LICENSE, DriverStatus.ACTIVE);
    when(routeRepository.existsVehicleOverlap(VEHICLE_ID, START, END)).thenReturn(true);

    assertThatThrownBy(() -> routeService.planRoute(request(), USER_ID))
        .isInstanceOf(ApiException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT)
        .hasFieldOrPropertyWithValue("code", "ROUTE_OVERLAP")
        .hasMessageContaining("Vehicle");
    verify(routeRepository, never()).save(any(Route.class));
  }

  @Test
  void planRoute_driverOverlap_throwsRouteOverlap() {
    stubVehicle(VehicleType.VAN, VehicleStatus.AVAILABLE);
    stubDriver(LicenseCategory.B, VALID_LICENSE, DriverStatus.ACTIVE);
    when(routeRepository.existsVehicleOverlap(VEHICLE_ID, START, END)).thenReturn(false);
    when(routeRepository.existsDriverOverlap(DRIVER_ID, START, END)).thenReturn(true);

    assertThatThrownBy(() -> routeService.planRoute(request(), USER_ID))
        .isInstanceOf(ApiException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT)
        .hasFieldOrPropertyWithValue("code", "ROUTE_OVERLAP")
        .hasMessageContaining("Driver");
    verify(routeRepository, never()).save(any(Route.class));
  }

  // ---------- planRoute: 404 ----------

  @Test
  void planRoute_vehicleNotFound_throwsVehicleNotFound() {
    when(vehicleClient.getVehicle(VEHICLE_ID)).thenThrow(notFound());

    assertApiError(
        () -> routeService.planRoute(request(), USER_ID),
        HttpStatus.NOT_FOUND,
        "VEHICLE_NOT_FOUND");
    verifyNoInteractions(driverClient);
    verify(routeRepository, never()).save(any(Route.class));
  }

  @Test
  void planRoute_driverNotFound_throwsDriverNotFound() {
    stubVehicle(VehicleType.VAN, VehicleStatus.AVAILABLE);
    when(driverClient.getDriver(DRIVER_ID)).thenThrow(notFound());

    assertApiError(
        () -> routeService.planRoute(request(), USER_ID), HttpStatus.NOT_FOUND, "DRIVER_NOT_FOUND");
    verify(routeRepository, never()).save(any(Route.class));
  }

  // ---------- planRoute: 503 SERVICE_UNAVAILABLE ----------

  @ParameterizedTest
  @MethodSource("downstreamFailures")
  void planRoute_vehiclesServiceDown_throwsServiceUnavailableAndSavesNothing(
      FeignException failure) {
    when(vehicleClient.getVehicle(VEHICLE_ID)).thenThrow(failure);

    assertThatThrownBy(() -> routeService.planRoute(request(), USER_ID))
        .isInstanceOf(ServiceUnavailableException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.SERVICE_UNAVAILABLE)
        .hasFieldOrPropertyWithValue("code", "SERVICE_UNAVAILABLE")
        .hasFieldOrPropertyWithValue("service", "ms-vehicles");
    verify(routeRepository, never()).save(any(Route.class));
  }

  @ParameterizedTest
  @MethodSource("downstreamFailures")
  void planRoute_driversServiceDown_throwsServiceUnavailableAndSavesNothing(
      FeignException failure) {
    stubVehicle(VehicleType.VAN, VehicleStatus.AVAILABLE);
    when(driverClient.getDriver(DRIVER_ID)).thenThrow(failure);

    assertThatThrownBy(() -> routeService.planRoute(request(), USER_ID))
        .isInstanceOf(ServiceUnavailableException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.SERVICE_UNAVAILABLE)
        .hasFieldOrPropertyWithValue("code", "SERVICE_UNAVAILABLE")
        .hasFieldOrPropertyWithValue("service", "ms-drivers");
    verify(routeRepository, never()).save(any(Route.class));
  }

  // ---------- getRoute ----------

  @Test
  void getRoute_existingRoute_returnsIt() {
    Route route =
        Route.builder()
            .id(ROUTE_ID)
            .vehicleId(VEHICLE_ID)
            .driverId(DRIVER_ID)
            .origin("Madrid - Centro Logístico")
            .destination("Valencia - Puerto")
            .plannedStart(START)
            .estimatedDurationMin(DURATION_MIN)
            .plannedDistanceKm(new BigDecimal("355.00"))
            .status(RouteStatus.PLANNED)
            .createdBy(USER_ID)
            .build();
    when(routeRepository.findById(ROUTE_ID)).thenReturn(Optional.of(route));

    RouteResponse response = routeService.getRoute(ROUTE_ID);

    assertThat(response.id()).isEqualTo(ROUTE_ID);
    assertThat(response.status()).isEqualTo(RouteStatus.PLANNED);
  }

  @Test
  void getRoute_unknownId_throwsRouteNotFound() {
    when(routeRepository.findById(ROUTE_ID)).thenReturn(Optional.empty());

    assertApiError(() -> routeService.getRoute(ROUTE_ID), HttpStatus.NOT_FOUND, "ROUTE_NOT_FOUND");
  }

  // ---------- listRoutes ----------

  @Test
  void listRoutes_returnsPageSortedByPlannedStartDescending() {
    Route route =
        Route.builder()
            .id(ROUTE_ID)
            .vehicleId(VEHICLE_ID)
            .driverId(DRIVER_ID)
            .origin("Madrid")
            .destination("Valencia")
            .plannedStart(START)
            .estimatedDurationMin(DURATION_MIN)
            .plannedDistanceKm(new BigDecimal("355.00"))
            .status(RouteStatus.COMPLETED)
            .createdBy(USER_ID)
            .build();
    ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
    Page<Route> page = new PageImpl<>(List.of(route), PageRequest.of(2, 5), 11);
    when(routeRepository.findAll(ArgumentMatchers.<Specification<Route>>any(), pageable.capture()))
        .thenReturn(page);

    PageResponse<RouteResponse> response =
        routeService.listRoutes(
            RouteStatus.COMPLETED,
            VEHICLE_ID,
            DRIVER_ID,
            LocalDate.of(2026, 10, 1),
            LocalDate.of(2026, 10, 31),
            2,
            5);

    assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
    assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
    assertThat(pageable.getValue().getSort().getOrderFor("plannedStart"))
        .isNotNull()
        .extracting(Sort.Order::getDirection)
        .isEqualTo(Sort.Direction.DESC);
    assertThat(response.content()).hasSize(1);
    assertThat(response.page()).isEqualTo(2);
    assertThat(response.size()).isEqualTo(5);
    assertThat(response.totalElements()).isEqualTo(11);
    assertThat(response.totalPages()).isEqualTo(3);
  }

  // ---------- helpers ----------

  private static CreateRouteRequest request() {
    return new CreateRouteRequest(
        VEHICLE_ID,
        DRIVER_ID,
        "Madrid - Centro Logístico",
        "Valencia - Puerto",
        START,
        DURATION_MIN,
        new BigDecimal("355.0"));
  }

  private void stubVehicle(VehicleType type, VehicleStatus status) {
    when(vehicleClient.getVehicle(VEHICLE_ID))
        .thenReturn(new VehicleResponse(VEHICLE_ID, type, status, 45210));
  }

  private void stubDriver(
      LicenseCategory category, LocalDate licenseExpiresAt, DriverStatus status) {
    when(driverClient.getDriver(DRIVER_ID))
        .thenReturn(new DriverResponse(DRIVER_ID, category, licenseExpiresAt, status));
  }

  private void stubNoOverlap() {
    when(routeRepository.existsVehicleOverlap(VEHICLE_ID, START, END)).thenReturn(false);
    when(routeRepository.existsDriverOverlap(DRIVER_ID, START, END)).thenReturn(false);
  }

  private static void assertApiError(ThrowingCallable call, HttpStatus status, String code) {
    assertThatThrownBy(call)
        .isInstanceOf(ApiException.class)
        .hasFieldOrPropertyWithValue("status", status)
        .hasFieldOrPropertyWithValue("code", code);
  }

  private static Request feignRequest() {
    return Request.create(
        Request.HttpMethod.GET,
        "http://downstream/api",
        Map.of(),
        (byte[]) null,
        StandardCharsets.UTF_8,
        null);
  }

  private static FeignException notFound() {
    return new FeignException.NotFound("not found", feignRequest(), new byte[0], Map.of());
  }

  /** Failures that mean "the dependency does not respond": connection/timeout and 5xx. */
  private static Stream<FeignException> downstreamFailures() {
    return Stream.of(
        new RetryableException(
            -1,
            "Connection refused",
            Request.HttpMethod.GET,
            new ConnectException("Connection refused"),
            (Long) null,
            feignRequest()),
        new FeignException.ServiceUnavailable(
            "unavailable", feignRequest(), new byte[0], Map.of()));
  }
}
