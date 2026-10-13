package com.fleetcontrol.msroutes.controller;

import com.fleetcontrol.msroutes.dto.CreateRouteRequest;
import com.fleetcontrol.msroutes.dto.PageResponse;
import com.fleetcontrol.msroutes.dto.RouteResponse;
import com.fleetcontrol.msroutes.enums.RouteStatus;
import com.fleetcontrol.msroutes.exeption.ApiException;
import com.fleetcontrol.msroutes.service.RouteService;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for route operations. */
@Validated
@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController implements RouteApi{

  private final RouteService routeService;

  @Override
  public RouteResponse planRoute(CreateRouteRequest request, Jwt jwt) {
    if (jwt == null) {
      throw new ApiException(
              HttpStatus.FORBIDDEN, "FORBIDDEN", "This operation requires an authentication user");
    }
    return routeService.planRoute(request, UUID.fromString(jwt.getSubject()));
  }

  @Override
  public PageResponse<RouteResponse> listRoutes(
          RouteStatus status,
          UUID vehicleId,
          UUID driverId,
          LocalDate from,
          LocalDate to,
          int page,
          int size) {
    return routeService.listRoutes(status, vehicleId, driverId, from, to, page, size);
  }

  @Override
  public RouteResponse getRoute(UUID routeId) {
    return routeService.getRoute(routeId);
  }

}
