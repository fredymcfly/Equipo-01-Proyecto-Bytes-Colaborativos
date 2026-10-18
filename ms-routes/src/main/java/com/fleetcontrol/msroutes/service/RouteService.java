package com.fleetcontrol.msroutes.service;

import com.fleetcontrol.msroutes.client.DriverClient;
import com.fleetcontrol.msroutes.client.VehicleClient;
import com.fleetcontrol.msroutes.client.dto.DriverResponse;
import com.fleetcontrol.msroutes.client.dto.VehicleResponse;
import com.fleetcontrol.msroutes.client.dto.VehicleStatusRequest;
import com.fleetcontrol.msroutes.client.enums.DriverStatus;
import com.fleetcontrol.msroutes.client.enums.VehicleStatus;
import com.fleetcontrol.msroutes.client.enums.VehicleType;
import com.fleetcontrol.msroutes.dto.CompleteRouteRequest;
import com.fleetcontrol.msroutes.dto.CreateRouteRequest;
import com.fleetcontrol.msroutes.dto.PageResponse;
import com.fleetcontrol.msroutes.dto.RouteResponse;
import com.fleetcontrol.msroutes.dto.RouteStatsResponse;
import com.fleetcontrol.msroutes.entities.Route;
import com.fleetcontrol.msroutes.enums.Granularity;
import com.fleetcontrol.msroutes.enums.RouteStatus;
import com.fleetcontrol.msroutes.exception.ApiException;
import com.fleetcontrol.msroutes.exception.ServiceUnavailableException;
import com.fleetcontrol.msroutes.repository.RouteRepository;
import com.fleetcontrol.msroutes.repository.RouteSpecification;
import com.fleetcontrol.msroutes.repository.RouteStatsRow;
import feign.FeignException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Plans, runs and queries routes. */
@Slf4j
@Service
@RequiredArgsConstructor
public class RouteService {

  private static final String VEHICLES_SERVICE = "ms-vehicles";
  private static final String DRIVERS_SERVICE = "ms-drivers";

  private final RouteRepository routeRepository;
  private final VehicleClient vehicleClient;
  private final DriverClient driverClient;
  private final Clock clock;

  /**
   * Plans a route after validating the vehicle, the driver and the time overlaps.
   *
   * @param request route data
   * @param createdBy id of the user who plans the route (JWT subject)
   * @return the route in PLANNED status
   */
  public RouteResponse planRoute(CreateRouteRequest request, UUID createdBy) {
    VehicleResponse vehicle = fetchVehicle(request.vehicleId());
    DriverResponse driver = fetchDriver(request.driverId());

    if (vehicle.status() == VehicleStatus.OUT_OF_SERVICE) {
      throw conflict("VEHICLE_NOT_AVAILABLE", "The selected vehicle is out of service");
    }
    checkDriverEligible(driver, vehicle.type(), request.plannedStart());
    checkNoOverlap(request);

    Route route =
        Route.builder()
            .vehicleId(request.vehicleId())
            .driverId(request.driverId())
            .origin(request.origin())
            .destination(request.destination())
            .plannedStart(request.plannedStart())
            .estimatedDurationMin(request.estimatedDurationMin())
            .plannedDistanceKm(request.plannedDistanceKm())
            .status(RouteStatus.PLANNED)
            .createdBy(createdBy)
            .build();
    return RouteResponse.from(routeRepository.save(route));
  }

  /** Returns a route by id or fails with ROUTE_NOT_FOUND. */
  public RouteResponse getRoute(UUID routeId) {
    return RouteResponse.from(findRoute(routeId));
  }

  /**
   * Lists routes ordered by planned start, newest first.
   *
   * @param from first day (inclusive, UTC) of the planned start; may be null
   * @param to last day (inclusive, UTC) of the planned start; may be null
   */
  public PageResponse<RouteResponse> listRoutes(
      RouteStatus status,
      UUID vehicleId,
      UUID driverId,
      LocalDate from,
      LocalDate to,
      int page,
      int size) {
    Instant fromInstant = from == null ? null : from.atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant toInstant = to == null ? null : to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "plannedStart"));

    return PageResponse.from(
        routeRepository
            .findAll(
                RouteSpecification.withFilters(status, vehicleId, driverId, fromInstant, toInstant),
                pageable)
            .map(RouteResponse::from));
  }

  /**
   * Starts a planned route: reserves the vehicle in ms-vehicles and saves the route as IN_PROGRESS.
   * If the route cannot be saved, the vehicle is released again.
   *
   * @param routeId route to start
   * @return the route in IN_PROGRESS status
   */
  public RouteResponse startRoute(UUID routeId) {
    Route route = findRoute(routeId);
    if (route.getStatus() != RouteStatus.PLANNED) {
      throw conflict("INVALID_ROUTE_STATE", "Only a planned route can be started");
    }
    VehicleResponse vehicle = fetchVehicle(route.getVehicleId());
    if (vehicle.status() != VehicleStatus.AVAILABLE) {
      throw conflict("VEHICLE_NOT_AVAILABLE", "The vehicle is in use or under maintenance");
    }
    changeVehicle(route.getVehicleId(), VehicleStatus.IN_USE, null);

    route.setStatus(RouteStatus.IN_PROGRESS);
    route.setStartedAt(clock.instant());
    route.setStartOdometerKm(vehicle.odometerKm());
    boolean saved = false;
    try {
      Route persisted = routeRepository.save(route);
      saved = true;
      return RouteResponse.from(persisted);
    } finally {
      if (!saved) {
        releaseVehicle(route.getVehicleId());
      }
    }
  }

  /**
   * Completes a route in progress and leaves its vehicle available with the new odometer. When
   * ms-vehicles does not answer the route stays IN_PROGRESS, so the call can be repeated.
   *
   * @param routeId route to complete
   * @param request real distance and notes
   * @return the route in COMPLETED status
   */
  public RouteResponse completeRoute(UUID routeId, CompleteRouteRequest request) {
    Route route = findRoute(routeId);
    if (route.getStatus() != RouteStatus.IN_PROGRESS) {
      throw conflict("INVALID_ROUTE_STATE", "Only a route in progress can be completed");
    }
    int endOdometer =
        route.getStartOdometerKm()
            + request.actualDistanceKm().setScale(0, RoundingMode.HALF_UP).intValueExact();

    VehicleResponse vehicle = fetchVehicle(route.getVehicleId());
    boolean alreadyReleased =
        vehicle.status() == VehicleStatus.AVAILABLE && vehicle.odometerKm() >= endOdometer;
    if (!alreadyReleased) {
      changeVehicle(route.getVehicleId(), VehicleStatus.AVAILABLE, endOdometer);
    }

    route.setStatus(RouteStatus.COMPLETED);
    route.setEndedAt(clock.instant());
    route.setEndOdometerKm(endOdometer);
    route.setActualDistanceKm(request.actualDistanceKm());
    route.setNotes(request.notes());
    return RouteResponse.from(routeRepository.save(route));
  }

  /**
   * Groups the routes completed between two days by their end date.
   *
   * @param from first day (inclusive, UTC)
   * @param to last day (inclusive, UTC)
   * @param granularity size of the periods; DAY when null
   * @param vehicleId vehicle to filter by; may be null
   */
  public RouteStatsResponse stats(
      LocalDate from, LocalDate to, Granularity granularity, UUID vehicleId) {
    if (from.isAfter(to)) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "The 'from' date must not be after 'to'");
    }
    Granularity unit = granularity == null ? Granularity.DAY : granularity;
    List<RouteStatsRow> rows =
        routeRepository.completedStats(
            unit.truncUnit(),
            from.atStartOfDay(ZoneOffset.UTC).toInstant(),
            to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant(),
            vehicleId == null ? null : vehicleId.toString());

    List<RouteStatsResponse.Point> series =
        rows.stream()
            .map(
                row ->
                    new RouteStatsResponse.Point(
                        row.getPeriod(),
                        row.getRoutes(),
                        row.getDistanceKm().setScale(1, RoundingMode.HALF_UP),
                        Math.round(row.getAvgDurationMin())))
            .toList();
    long routes = series.stream().mapToLong(RouteStatsResponse.Point::routes).sum();
    BigDecimal distance =
        series.stream()
            .map(RouteStatsResponse.Point::distanceKm)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    return new RouteStatsResponse(
        from, to, unit, new RouteStatsResponse.Totals(routes, distance), series);
  }

  private Route findRoute(UUID routeId) {
    return routeRepository
        .findById(routeId)
        .orElseThrow(
            () ->
                new ApiException(
                    HttpStatus.NOT_FOUND,
                    "ROUTE_NOT_FOUND",
                    "No route was found with the provided id."));
  }

  private void changeVehicle(UUID vehicleId, VehicleStatus status, Integer odometerKm) {
    try {
      vehicleClient.updateStatus(vehicleId, new VehicleStatusRequest(status, odometerKm));
    } catch (FeignException | NoFallbackAvailableException e) {
      throw unavailable(VEHICLES_SERVICE, e);
    }
  }

  /** Compensation of {@link #startRoute}: the failure of the release is only logged. */
  private void releaseVehicle(UUID vehicleId) {
    try {
      vehicleClient.updateStatus(
          vehicleId, new VehicleStatusRequest(VehicleStatus.AVAILABLE, null));
    } catch (FeignException | NoFallbackAvailableException e) {
      log.error("The vehicle {} could not be released: {}", vehicleId, e.getMessage());
    }
  }

  private VehicleResponse fetchVehicle(UUID vehicleId) {
    try {
      return vehicleClient.getVehicle(vehicleId);
    } catch (FeignException | NoFallbackAvailableException e) {
      if (isNotFound(e)) {
        throw new ApiException(
            HttpStatus.NOT_FOUND,
            "VEHICLE_NOT_FOUND",
            "No vehicle was found with the provided id.");
      }
      throw unavailable(VEHICLES_SERVICE, e);
    }
  }

  private DriverResponse fetchDriver(UUID driverId) {
    try {
      return driverClient.getDriver(driverId);
    } catch (FeignException | NoFallbackAvailableException e) {
      if (isNotFound(e)) {
        throw new ApiException(
            HttpStatus.NOT_FOUND, "DRIVER_NOT_FOUND", "No driver was found with the provided id.");
      }
      throw unavailable(DRIVERS_SERVICE, e);
    }
  }

  private void checkDriverEligible(DriverResponse driver, VehicleType type, Instant start) {
    if (driver.status() != DriverStatus.ACTIVE) {
      throw conflict("DRIVER_NOT_ELIGIBLE", "Driver is not active");
    }
    LocalDate startDate = start.atZone(ZoneOffset.UTC).toLocalDate();
    if (driver.licenseExpiresAt().isBefore(startDate)) {
      throw conflict("DRIVER_NOT_ELIGIBLE", "The driver's license is not valid on the start date");
    }
    if (!driver.licenseCategory().allows(type)) {
      throw conflict(
          "DRIVER_NOT_ELIGIBLE", "The license category is invalid for the selected vehicle type");
    }
  }

  private void checkNoOverlap(CreateRouteRequest request) {
    Instant start = request.plannedStart();
    Instant end = start.plus(Duration.ofMinutes(request.estimatedDurationMin()));
    if (routeRepository.existsVehicleOverlap(request.vehicleId(), start, end)) {
      throw conflict(
          "ROUTE_OVERLAP",
          "Vehicle unavailable: an existing route overlaps the specified time interval");
    }
    if (routeRepository.existsDriverOverlap(request.driverId(), start, end)) {
      throw conflict(
          "ROUTE_OVERLAP",
          "Driver unavailable: an existing route overlaps the specified time interval");
    }
  }

  private ApiException conflict(String code, String message) {
    return new ApiException(HttpStatus.CONFLICT, code, message);
  }

  /**
   * With the circuit breaker every failure of a client arrives wrapped in a {@link
   * NoFallbackAvailableException}, so a 404 has to be looked for in its cause.
   */
  private static boolean isNotFound(RuntimeException exception) {
    Throwable cause =
        exception instanceof NoFallbackAvailableException && exception.getCause() != null
            ? exception.getCause()
            : exception;
    return cause instanceof FeignException.NotFound;
  }

  private ServiceUnavailableException unavailable(String service, RuntimeException cause) {
    log.warn("{} no response: {}", service, cause.getMessage());
    return new ServiceUnavailableException(service);
  }
}
