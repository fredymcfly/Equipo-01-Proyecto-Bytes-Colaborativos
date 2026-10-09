package com.fleetcontrol.msroutes.controller;

import com.fleetcontrol.msroutes.dto.CreateRouteRequest;
import com.fleetcontrol.msroutes.dto.PageResponse;
import com.fleetcontrol.msroutes.dto.RouteResponse;
import com.fleetcontrol.msroutes.enums.RouteStatus;
import com.fleetcontrol.msroutes.exeption.ApiException;
import com.fleetcontrol.msroutes.service.RouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * REST controller for route operations.
 * */
@Validated
@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
@Tag(name = "Routes", description= "Planning and querying of fleet routes")
public class RouteController {

  private final RouteService routeService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Plans a route for a vehicle and a driver")
  @ApiResponse(responseCode = "201", description = "Route created in PLANNED status")
  @ApiResponse(responseCode = "404", description = "VEHICLE_NOT_FOUND or DRIVER_NOT_FOUND")
  @ApiResponse(responseCode = "409", description = "VEHICLE_NOT_AVAILABLE, DRIVER_NOT_ELEGIBLE or ROUTE_OVERLAP")
  @ApiResponse(responseCode = "503", description = "ms-vehicle or ms-drivers not responding")
  public RouteResponse planRoute(
          @Valid @RequestBody CreateRouteRequest request,
          @AuthenticationPrincipal Jwt jwt){
    if (jwt == null){
      throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "This operation requires an authentication user");
    }
    return routeService.planRoute(request, UUID.fromString(jwt.getSubject()));
  }

  @GetMapping
  @Operation(summary = "List routes with filters, newest planned start first")
  public PageResponse<RouteResponse> listRoutes(
          @RequestParam(required = false)RouteStatus status,
          @RequestParam(name="vehicle", required = false) UUID vehicleId,
          @RequestParam(name = "driver", required = false) UUID driverId,
          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
          @RequestParam(defaultValue = "0") @Min(0) int page,
          @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
            return routeService.listRoutes(status, vehicleId, driverId, from, to, page, size);
  }

  @GetMapping("/{routeId}")
  @Operation(summary = "Gets a route by id")
  @ApiResponse(responseCode = "404", description = "ROUTE_NOT_FOUND")
  public RouteResponse getRoute(@PathVariable UUID routeId){
            return routeService.getRoute(routeId);
  }



}
