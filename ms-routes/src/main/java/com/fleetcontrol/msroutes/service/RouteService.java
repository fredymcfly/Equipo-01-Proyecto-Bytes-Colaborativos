package com.fleetcontrol.msroutes.service;

import com.fleetcontrol.msroutes.client.DriverClient;
import com.fleetcontrol.msroutes.client.VehicleClient;
import com.fleetcontrol.msroutes.client.dto.DriverResponse;
import com.fleetcontrol.msroutes.client.dto.VehicleResponse;
import com.fleetcontrol.msroutes.client.enums.DriverStatus;
import com.fleetcontrol.msroutes.client.enums.VehicleStatus;
import com.fleetcontrol.msroutes.client.enums.VehicleType;
import com.fleetcontrol.msroutes.dto.CreateRouteRequest;
import com.fleetcontrol.msroutes.dto.PageResponse;
import com.fleetcontrol.msroutes.dto.RouteResponse;
import com.fleetcontrol.msroutes.entities.Route;
import com.fleetcontrol.msroutes.enums.RouteStatus;
import com.fleetcontrol.msroutes.exeption.ApiException;
import com.fleetcontrol.msroutes.exeption.ServiceUnavailableException;
import com.fleetcontrol.msroutes.repository.RouteRepository;
import com.fleetcontrol.msroutes.repository.RouteSpecification;
import feign.FeignException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Plans and queries routes. */
@Slf4j
@Service
@RequiredArgsConstructor
public class RouteService {

  private static final String VEHICLES_SERVICE = "ms-vehicle";
  private static final String DRIVERS_SERVICE = "ms-driver";

  private final RouteRepository routeRepository;
  private final VehicleClient vehicleClient;
  private final DriverClient driverClient;

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
    return routeRepository
        .findById(routeId)
        .map(RouteResponse::from)
        .orElseThrow(
            () ->
                new ApiException(
                    HttpStatus.NOT_FOUND,
                    "ROUTE_NOT_FOUND",
                    "No exists a route with the Id provided"));
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

  private VehicleResponse fetchVehicle(UUID vehicleId) {
    try {
      return vehicleClient.getVehicle(vehicleId);
    } catch (FeignException.NotFound e) {
      throw new ApiException(
          HttpStatus.NOT_FOUND, "VEHICLE_NOT_FOUND", "No vehicle was found with the provided id.");
    } catch (FeignException e) {
      throw unavailable(VEHICLES_SERVICE, e);
    }
  }

  private DriverResponse fetchDriver(UUID driverId) {
    try {
      return driverClient.getDriver(driverId);
    } catch (FeignException.NotFound e) {
      throw new ApiException(
          HttpStatus.NOT_FOUND, "DRIVER_NOT_FOUND", "No driver was found with the provided id.");
    } catch (FeignException e) {
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
          "Vehicle unavailable: an existing route overlap with the specified time interval");
    }
    if (routeRepository.existsDriverOverlap(request.driverId(), start, end)) {
      throw conflict(
          "ROUTE_OVERLAP",
          "Driver unavailable: an existing route overlap with the specified time interval");
    }
  }

  private ApiException conflict(String code, String message) {
    return new ApiException(HttpStatus.CONFLICT, code, message);
  }

  private ServiceUnavailableException unavailable(String service, FeignException cause) {
    log.warn("{} no response: {}", service, cause.getMessage());
    return new ServiceUnavailableException(service);
  }
}
