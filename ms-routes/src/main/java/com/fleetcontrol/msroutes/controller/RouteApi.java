package com.fleetcontrol.msroutes.controller;

import com.fleetcontrol.msroutes.dto.CompleteRouteRequest;
import com.fleetcontrol.msroutes.dto.CreateRouteRequest;
import com.fleetcontrol.msroutes.dto.PageResponse;
import com.fleetcontrol.msroutes.dto.RouteResponse;
import com.fleetcontrol.msroutes.dto.RouteStatsResponse;
import com.fleetcontrol.msroutes.enums.Granularity;
import com.fleetcontrol.msroutes.enums.RouteStatus;
import com.fleetcontrol.msroutes.exception.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Contract and Swagger documentation of the route endpoints.
 *
 * <p>The mappings, the parameter constraints and the OpenAPI annotations live here so that {@link
 * RouteController} only contains the logic. The controller must not repeat them.
 */
@Tag(name = "Routes", description = "Planning and querying of fleet routes")
@SecurityRequirement(name = "bearerAuth")
public interface RouteApi {

  /**
   * Plans a route for the authenticated user.
   *
   * @param request route data
   * @param jwt authenticated user's JWT
   * @return created route
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Plans a route for a vehicle and a driver",
      description =
          "Validates the vehicle and the driver against ms-vehicles and ms-drivers and checks "
              + "that neither of them has an overlapping PLANNED or IN_PROGRESS route. "
              + RouteApiDocs.REQUIRES_MANAGER)
  @ApiResponse(
      responseCode = "201",
      description = "Route created in PLANNED status",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              examples =
                  @ExampleObject(name = "Planned route", value = RouteExamples.ROUTE_PLANNED)))
  @ApiResponse(
      responseCode = RouteApiDocs.CODE_400,
      description = "VALIDATION_ERROR: a field of the body is missing or invalid",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ApiError.class),
              examples =
                  @ExampleObject(name = "Invalid body", value = RouteExamples.ERR_VALIDATION)))
  @ApiResponse(
      responseCode = RouteApiDocs.CODE_401,
      description = RouteApiDocs.DESC_UNAUTHORIZED,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ApiError.class),
              examples = {
                @ExampleObject(
                    name = RouteApiDocs.EXAMPLE_MISSING_TOKEN,
                    value = RouteExamples.ERR_UNAUTHORIZED),
                @ExampleObject(
                    name = RouteApiDocs.EXAMPLE_EXPIRED_TOKEN,
                    value = RouteExamples.ERR_TOKEN_EXPIRED)
              }))
  @ApiResponse(
      responseCode = RouteApiDocs.CODE_403,
      description = RouteApiDocs.DESC_FORBIDDEN,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ApiError.class),
              examples =
                  @ExampleObject(
                      name = RouteApiDocs.EXAMPLE_FORBIDDEN,
                      value = RouteExamples.ERR_FORBIDDEN)))
  @ApiResponse(
      responseCode = RouteApiDocs.CODE_404,
      description = "VEHICLE_NOT_FOUND or DRIVER_NOT_FOUND",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ApiError.class),
              examples = {
                @ExampleObject(
                    name = "Vehicle not found",
                    value = RouteExamples.ERR_VEHICLE_NOT_FOUND),
                @ExampleObject(
                    name = "Driver not found",
                    value = RouteExamples.ERR_DRIVER_NOT_FOUND)
              }))
  @ApiResponse(
      responseCode = RouteApiDocs.CODE_409,
      description = "VEHICLE_NOT_AVAILABLE, DRIVER_NOT_ELIGIBLE or ROUTE_OVERLAP",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ApiError.class),
              examples = {
                @ExampleObject(
                    name = "Vehicle out of service",
                    value = RouteExamples.ERR_VEHICLE_NOT_AVAILABLE),
                @ExampleObject(
                    name = "Driver not active",
                    value = RouteExamples.ERR_DRIVER_INACTIVE),
                @ExampleObject(
                    name = "License expired on start date",
                    value = RouteExamples.ERR_LICENSE_EXPIRED),
                @ExampleObject(
                    name = "License category not valid for the vehicle",
                    value = RouteExamples.ERR_LICENSE_CATEGORY),
                @ExampleObject(name = "Vehicle overlap", value = RouteExamples.ERR_VEHICLE_OVERLAP),
                @ExampleObject(name = "Driver overlap", value = RouteExamples.ERR_DRIVER_OVERLAP)
              }))
  @ApiResponse(
      responseCode = RouteApiDocs.CODE_503,
      description = "SERVICE_UNAVAILABLE: ms-vehicles or ms-drivers is not responding",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ApiError.class),
              examples =
                  @ExampleObject(
                      name = "Dependency down",
                      value = RouteExamples.ERR_SERVICE_UNAVAILABLE)))
  RouteResponse planRoute(
      @Valid @RequestBody CreateRouteRequest request,
      @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt);

  /**
   * Lists routes with optional filters, newest planned start first.
   *
   * @param status route status
   * @param vehicleId assigned vehicle
   * @param driverId assigned driver
   * @param from first day (inclusive) of the planned start
   * @param to last day (inclusive) of the planned start
   * @param page zero-based page number
   * @param size page size
   * @return page of routes
   */
  @GetMapping
  @Operation(
      summary = "List routes with filters, newest planned start first",
      description =
          "All filters are optional and can be combined. `from` and `to` are inclusive days "
              + "(UTC) applied to the planned start. Also available for internal service calls.")
  @ApiResponse(
      responseCode = RouteApiDocs.CODE_200,
      description = "Page of routes",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              examples = @ExampleObject(name = "Page of routes", value = RouteExamples.ROUTE_PAGE)))
  @ApiResponse(
      responseCode = RouteApiDocs.CODE_400,
      description = "VALIDATION_ERROR: a query parameter is invalid",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ApiError.class),
              examples =
                  @ExampleObject(
                      name = "Invalid parameter",
                      value = RouteExamples.ERR_VALIDATION_PARAMS)))
  @ApiResponse(
      responseCode = RouteApiDocs.CODE_401,
      description = RouteApiDocs.DESC_UNAUTHORIZED,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ApiError.class),
              examples = {
                @ExampleObject(
                    name = RouteApiDocs.EXAMPLE_MISSING_TOKEN,
                    value = RouteExamples.ERR_UNAUTHORIZED),
                @ExampleObject(
                    name = RouteApiDocs.EXAMPLE_EXPIRED_TOKEN,
                    value = RouteExamples.ERR_TOKEN_EXPIRED)
              }))
  @ApiResponse(
      responseCode = RouteApiDocs.CODE_403,
      description = RouteApiDocs.DESC_FORBIDDEN,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ApiError.class),
              examples =
                  @ExampleObject(
                      name = RouteApiDocs.EXAMPLE_FORBIDDEN,
                      value = RouteExamples.ERR_FORBIDDEN)))
  PageResponse<RouteResponse> listRoutes(
      @Parameter(description = "Filter by route status", example = "COMPLETED")
          @RequestParam(required = false)
          RouteStatus status,
      @Parameter(
              description = "Filter by vehicle id",
              example = "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01")
          @RequestParam(name = "vehicle", required = false)
          UUID vehicleId,
      @Parameter(
              description = "Filter by driver id",
              example = "3b2d8f10-7c4a-4e55-b1a0-5d9e2c7f1a01")
          @RequestParam(name = "driver", required = false)
          UUID driverId,
      @Parameter(description = "First day (inclusive) of the planned start", example = "2026-10-01")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate from,
      @Parameter(description = "Last day (inclusive) of the planned start", example = "2026-10-31")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate to,
      @Parameter(description = "Zero-based page number", example = "0")
          @RequestParam(defaultValue = "0")
          @Min(0)
          int page,
      @Parameter(description = "Page size (1 to 100)", example = "20")
          @RequestParam(defaultValue = "20")
          @Min(1)
          @Max(100)
          int size);

  /**
   * Gets a route by id.
   *
   * @param routeId route identifier
   * @return the route
   */
  @GetMapping("/{routeId}")
  @Operation(
      summary = "Gets a route by id",
      description = "Also available for internal service calls.")
  @ApiResponse(
      responseCode = RouteApiDocs.CODE_200,
      description = "Route found",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              examples =
                  @ExampleObject(name = "Completed route", value = RouteExamples.ROUTE_COMPLETED)))
  @ApiResponse(
      responseCode = RouteApiDocs.CODE_401,
      description = RouteApiDocs.DESC_UNAUTHORIZED,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ApiError.class),
              examples = {
                @ExampleObject(
                    name = RouteApiDocs.EXAMPLE_MISSING_TOKEN,
                    value = RouteExamples.ERR_UNAUTHORIZED),
                @ExampleObject(
                    name = RouteApiDocs.EXAMPLE_EXPIRED_TOKEN,
                    value = RouteExamples.ERR_TOKEN_EXPIRED)
              }))
  @ApiResponse(
      responseCode = RouteApiDocs.CODE_403,
      description = RouteApiDocs.DESC_FORBIDDEN,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ApiError.class),
              examples =
                  @ExampleObject(
                      name = RouteApiDocs.EXAMPLE_FORBIDDEN,
                      value = RouteExamples.ERR_FORBIDDEN)))
  @ApiResponse(
      responseCode = RouteApiDocs.CODE_404,
      description = RouteApiDocs.DESC_ROUTE_NOT_FOUND_CODE,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ApiError.class),
              examples =
                  @ExampleObject(
                      name = RouteApiDocs.EXAMPLE_ROUTE_NOT_FOUND,
                      value = RouteExamples.ERR_ROUTE_NOT_FOUND)))
  RouteResponse getRoute(
      @Parameter(description = RouteApiDocs.PARAM_ROUTE_ID, example = RouteApiDocs.ROUTE_ID_EXAMPLE)
          @PathVariable
          UUID routeId);

  /**
   * Starts a planned route.
   *
   * @param routeId route identifier
   * @return the route in IN_PROGRESS status
   */
  @PostMapping("/{routeId}/start")
  @Operation(
      summary = "Starts a planned route",
      description =
          "Checks that the route is PLANNED and its vehicle AVAILABLE, sets the vehicle to "
              + "IN_USE and saves the route as IN_PROGRESS with the current time and the "
              + "odometer of the vehicle. If the route cannot be saved, the vehicle is released. "
              + RouteApiDocs.REQUIRES_MANAGER)
  @ApiResponse(
      responseCode = RouteApiDocs.CODE_200,
      description = "Route in progress",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              examples =
                  @ExampleObject(name = "In progress", value = RouteExamples.ROUTE_IN_PROGRESS)))
  @ApiResponse(
      responseCode = RouteApiDocs.CODE_401,
      description = RouteApiDocs.DESC_UNAUTHORIZED,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ApiError.class),
              examples = {
                @ExampleObject(
                    name = RouteApiDocs.EXAMPLE_MISSING_TOKEN,
                    value = RouteExamples.ERR_UNAUTHORIZED),
                @ExampleObject(
                    name = RouteApiDocs.EXAMPLE_EXPIRED_TOKEN,
                    value = RouteExamples.ERR_TOKEN_EXPIRED)
              }))
  @ApiResponse(
      responseCode = RouteApiDocs.CODE_403,
      description = RouteApiDocs.DESC_FORBIDDEN,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ApiError.class),
              examples =
                  @ExampleObject(
                      name = RouteApiDocs.EXAMPLE_FORBIDDEN,
                      value = RouteExamples.ERR_FORBIDDEN)))
  @ApiResponse(
      responseCode = RouteApiDocs.CODE_404,
      description = RouteApiDocs.DESC_ROUTE_NOT_FOUND_CODE,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ApiError.class),
              examples =
                  @ExampleObject(
                      name = RouteApiDocs.EXAMPLE_ROUTE_NOT_FOUND,
                      value = RouteExamples.ERR_ROUTE_NOT_FOUND)))
  @ApiResponse(
      responseCode = RouteApiDocs.CODE_409,
      description = "INVALID_ROUTE_STATE or VEHICLE_NOT_AVAILABLE",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ApiError.class),
              examples =
                  @ExampleObject(
                      name = "Route not planned",
                      value = RouteExamples.ERR_INVALID_ROUTE_STATE)))
  @ApiResponse(
      responseCode = RouteApiDocs.CODE_503,
      description = "SERVICE_UNAVAILABLE: ms-vehicles does not answer",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ApiError.class),
              examples =
                  @ExampleObject(
                      name = "Service unavailable",
                      value = RouteExamples.ERR_SERVICE_UNAVAILABLE)))
  RouteResponse startRoute(
      @Parameter(description = RouteApiDocs.PARAM_ROUTE_ID, example = RouteApiDocs.ROUTE_ID_EXAMPLE)
          @PathVariable
          UUID routeId);

  /**
   * Completes a route in progress.
   *
   * @param routeId route identifier
   * @param request real distance and notes
   * @return the route in COMPLETED status
   */
  @PostMapping("/{routeId}/complete")
  @Operation(
      summary = "Completes a route in progress",
      description =
          "Adds the rounded actual distance to the start odometer, leaves the vehicle AVAILABLE "
              + "with the new odometer and saves the route as COMPLETED. If ms-vehicles does not "
              + "answer, the route stays IN_PROGRESS and the call can be repeated. "
              + RouteApiDocs.REQUIRES_MANAGER)
  @ApiResponse(
      responseCode = RouteApiDocs.CODE_200,
      description = "Route completed",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              examples =
                  @ExampleObject(name = "Completed route", value = RouteExamples.ROUTE_COMPLETED)))
  @ApiResponse(
      responseCode = RouteApiDocs.CODE_400,
      description = "VALIDATION_ERROR: actualDistanceKm must be greater than 0",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ApiError.class),
              examples =
                  @ExampleObject(name = "Invalid body", value = RouteExamples.ERR_VALIDATION)))
  @ApiResponse(
      responseCode = RouteApiDocs.CODE_401,
      description = RouteApiDocs.DESC_UNAUTHORIZED,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ApiError.class),
              examples = {
                @ExampleObject(
                    name = RouteApiDocs.EXAMPLE_MISSING_TOKEN,
                    value = RouteExamples.ERR_UNAUTHORIZED),
                @ExampleObject(
                    name = RouteApiDocs.EXAMPLE_EXPIRED_TOKEN,
                    value = RouteExamples.ERR_TOKEN_EXPIRED)
              }))
  @ApiResponse(
      responseCode = RouteApiDocs.CODE_403,
      description = RouteApiDocs.DESC_FORBIDDEN,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ApiError.class),
              examples =
                  @ExampleObject(
                      name = RouteApiDocs.EXAMPLE_FORBIDDEN,
                      value = RouteExamples.ERR_FORBIDDEN)))
  @ApiResponse(
      responseCode = RouteApiDocs.CODE_404,
      description = RouteApiDocs.DESC_ROUTE_NOT_FOUND_CODE,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ApiError.class),
              examples =
                  @ExampleObject(
                      name = RouteApiDocs.EXAMPLE_ROUTE_NOT_FOUND,
                      value = RouteExamples.ERR_ROUTE_NOT_FOUND)))
  @ApiResponse(
      responseCode = RouteApiDocs.CODE_409,
      description = "INVALID_ROUTE_STATE: the route is not in progress",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ApiError.class),
              examples =
                  @ExampleObject(
                      name = "Route not in progress",
                      value = RouteExamples.ERR_INVALID_ROUTE_STATE)))
  @ApiResponse(
      responseCode = RouteApiDocs.CODE_503,
      description = "SERVICE_UNAVAILABLE: ms-vehicles does not answer",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ApiError.class),
              examples =
                  @ExampleObject(
                      name = "Service unavailable",
                      value = RouteExamples.ERR_SERVICE_UNAVAILABLE)))
  RouteResponse completeRoute(
      @Parameter(description = RouteApiDocs.PARAM_ROUTE_ID, example = RouteApiDocs.ROUTE_ID_EXAMPLE)
          @PathVariable
          UUID routeId,
      @Valid
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      examples =
                          @ExampleObject(
                              name = "Complete",
                              value = RouteExamples.COMPLETE_REQUEST)))
          @RequestBody
          CompleteRouteRequest request);

  /**
   * Returns the routes completed in a range, grouped by their end date.
   *
   * @param from first day (inclusive)
   * @param to last day (inclusive)
   * @param granularity size of the periods
   * @param vehicleId optional vehicle filter
   * @return totals and one point per period
   */
  @GetMapping("/stats")
  @Operation(
      summary = "Statistics of the completed routes",
      description =
          "Time series of the routes completed between two days, grouped by their end date "
              + "(UTC). Weeks start on Monday. Also available for internal service calls.")
  @ApiResponse(
      responseCode = RouteApiDocs.CODE_200,
      description = "Totals and series",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = RouteStatsResponse.class),
              examples = @ExampleObject(name = "Weekly", value = RouteExamples.ROUTE_STATS)))
  @ApiResponse(
      responseCode = RouteApiDocs.CODE_400,
      description = "VALIDATION_ERROR: missing or invalid parameters",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ApiError.class),
              examples =
                  @ExampleObject(
                      name = "Invalid params",
                      value = RouteExamples.ERR_VALIDATION_PARAMS)))
  @ApiResponse(
      responseCode = RouteApiDocs.CODE_401,
      description = RouteApiDocs.DESC_UNAUTHORIZED,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ApiError.class),
              examples = {
                @ExampleObject(
                    name = RouteApiDocs.EXAMPLE_MISSING_TOKEN,
                    value = RouteExamples.ERR_UNAUTHORIZED),
                @ExampleObject(
                    name = RouteApiDocs.EXAMPLE_EXPIRED_TOKEN,
                    value = RouteExamples.ERR_TOKEN_EXPIRED)
              }))
  @ApiResponse(
      responseCode = RouteApiDocs.CODE_403,
      description = RouteApiDocs.DESC_FORBIDDEN,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ApiError.class),
              examples =
                  @ExampleObject(
                      name = RouteApiDocs.EXAMPLE_FORBIDDEN,
                      value = RouteExamples.ERR_FORBIDDEN)))
  RouteStatsResponse stats(
      @Parameter(description = "First day (inclusive) of the end date", example = "2026-09-07")
          @RequestParam
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate from,
      @Parameter(description = "Last day (inclusive) of the end date", example = "2026-09-27")
          @RequestParam
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate to,
      @Parameter(description = "Size of the periods", example = "WEEK")
          @RequestParam(defaultValue = "DAY")
          Granularity granularity,
      @Parameter(
              description = "Filter by vehicle id",
              example = "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01")
          @RequestParam(name = "vehicle", required = false)
          UUID vehicleId);
}
