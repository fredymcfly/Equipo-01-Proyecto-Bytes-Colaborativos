package com.fleetcontrol.msvehicles.controller;

import com.fleetcontrol.msvehicles.constants.ApiPaths;
import com.fleetcontrol.msvehicles.constants.BusinessConstants;
import com.fleetcontrol.msvehicles.constants.SwaggerConstants;
import com.fleetcontrol.msvehicles.documentation.CommonErrorResponses;
import com.fleetcontrol.msvehicles.models.dto.request.CreateVehicleRequest;
import com.fleetcontrol.msvehicles.models.dto.request.UpdateVehicleRequest;
import com.fleetcontrol.msvehicles.models.dto.request.VehicleStatusRequest;
import com.fleetcontrol.msvehicles.models.dto.response.ErrorResponse;
import com.fleetcontrol.msvehicles.models.dto.response.FleetSummaryResponse;
import com.fleetcontrol.msvehicles.models.dto.response.PageResponse;
import com.fleetcontrol.msvehicles.models.dto.response.VehicleResponse;
import com.fleetcontrol.msvehicles.models.dto.response.VehicleSummaryResponse;
import com.fleetcontrol.msvehicles.models.enums.VehicleStatus;
import com.fleetcontrol.msvehicles.models.enums.VehicleType;
import com.fleetcontrol.msvehicles.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoints for the vehicle catalogue. */
@RestController
@RequestMapping(ApiPaths.Vehicles.BASE)
@Validated
@Tag(
    name = SwaggerConstants.Tags.VEHICLES,
    description = SwaggerConstants.Tags.VEHICLES_DESCRIPTION)
@SecurityRequirements({
  @SecurityRequirement(name = SwaggerConstants.Security.BEARER),
  @SecurityRequirement(name = SwaggerConstants.Security.INTERNAL_KEY)
})
@RequiredArgsConstructor
public class VehicleController {

  private final VehicleService vehicleService;

  /** Lists vehicles with optional filters and pagination. */
  @GetMapping
  @Operation(
      summary = SwaggerConstants.Operations.LIST,
      description = SwaggerConstants.Operations.LIST_DESCRIPTION)
  @ApiResponse(
      responseCode = SwaggerConstants.Status.OK,
      description = SwaggerConstants.Responses.OK,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              examples = @ExampleObject(value = SwaggerConstants.Examples.VEHICLE_PAGE)))
  @CommonErrorResponses
  public PageResponse<VehicleSummaryResponse> list(
      @Parameter(description = SwaggerConstants.Params.STATUS) @RequestParam(required = false)
          VehicleStatus status,
      @Parameter(description = SwaggerConstants.Params.TYPE) @RequestParam(required = false)
          VehicleType type,
      @Parameter(description = SwaggerConstants.Params.PLATE) @RequestParam(required = false)
          String plate,
      @Parameter(description = SwaggerConstants.Params.PAGE)
          @RequestParam(defaultValue = BusinessConstants.Pagination.DEFAULT_PAGE)
          @Min(BusinessConstants.Pagination.MIN_PAGE)
          int page,
      @Parameter(description = SwaggerConstants.Params.SIZE)
          @RequestParam(defaultValue = BusinessConstants.Pagination.DEFAULT_SIZE)
          @Min(BusinessConstants.Pagination.MIN_SIZE)
          @Max(BusinessConstants.Pagination.MAX_SIZE)
          int size) {
    return vehicleService.list(status, type, plate, page, size);
  }

  /** Returns the number of vehicles by status and by type. */
  @GetMapping(ApiPaths.Vehicles.SUMMARY)
  @Operation(
      summary = SwaggerConstants.Operations.SUMMARY,
      description = SwaggerConstants.Operations.SUMMARY_DESCRIPTION)
  @ApiResponse(
      responseCode = SwaggerConstants.Status.OK,
      description = SwaggerConstants.Responses.OK,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              examples = @ExampleObject(value = SwaggerConstants.Examples.FLEET_SUMMARY)))
  @CommonErrorResponses
  public FleetSummaryResponse summary() {
    return vehicleService.summary();
  }

  /** Returns the detailed data of one vehicle. */
  @GetMapping(ApiPaths.Vehicles.VEHICLE_ID)
  @Operation(
      summary = SwaggerConstants.Operations.GET,
      description = SwaggerConstants.Operations.GET_DESCRIPTION)
  @ApiResponse(
      responseCode = SwaggerConstants.Status.OK,
      description = SwaggerConstants.Responses.OK,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              examples = @ExampleObject(value = SwaggerConstants.Examples.VEHICLE)))
  @ApiResponse(
      responseCode = SwaggerConstants.Status.NOT_FOUND,
      description = SwaggerConstants.Responses.NOT_FOUND,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(value = SwaggerConstants.Examples.ERROR_NOT_FOUND)))
  @CommonErrorResponses
  public VehicleResponse get(
      @Parameter(description = SwaggerConstants.Params.VEHICLE_ID) @PathVariable UUID vehicleId) {
    return vehicleService.getById(vehicleId);
  }

  /** Registers a new vehicle. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = SwaggerConstants.Operations.CREATE,
      description = SwaggerConstants.Operations.CREATE_DESCRIPTION,
      requestBody =
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      examples = @ExampleObject(value = SwaggerConstants.Examples.CREATE_REQUEST))))
  @ApiResponse(
      responseCode = SwaggerConstants.Status.CREATED,
      description = SwaggerConstants.Responses.CREATED,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              examples = @ExampleObject(value = SwaggerConstants.Examples.VEHICLE)))
  @ApiResponse(
      responseCode = SwaggerConstants.Status.CONFLICT,
      description = SwaggerConstants.Responses.CREATE_CONFLICT,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(value = SwaggerConstants.Examples.ERROR_PLATE_EXISTS)))
  @CommonErrorResponses
  public VehicleResponse create(@Valid @RequestBody CreateVehicleRequest request) {
    return vehicleService.create(request);
  }

  /** Updates the descriptive data of a vehicle. */
  @PutMapping(ApiPaths.Vehicles.VEHICLE_ID)
  @Operation(
      summary = SwaggerConstants.Operations.UPDATE,
      description = SwaggerConstants.Operations.UPDATE_DESCRIPTION,
      requestBody =
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      examples = @ExampleObject(value = SwaggerConstants.Examples.UPDATE_REQUEST))))
  @ApiResponse(
      responseCode = SwaggerConstants.Status.OK,
      description = SwaggerConstants.Responses.OK,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              examples = @ExampleObject(value = SwaggerConstants.Examples.VEHICLE)))
  @ApiResponse(
      responseCode = SwaggerConstants.Status.NOT_FOUND,
      description = SwaggerConstants.Responses.NOT_FOUND,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(value = SwaggerConstants.Examples.ERROR_NOT_FOUND)))
  @CommonErrorResponses
  public VehicleResponse update(
      @Parameter(description = SwaggerConstants.Params.VEHICLE_ID) @PathVariable UUID vehicleId,
      @Valid @RequestBody UpdateVehicleRequest request) {
    return vehicleService.update(vehicleId, request);
  }

  /** Changes the status of a vehicle and, optionally, its odometer. */
  @PatchMapping(ApiPaths.Vehicles.VEHICLE_ID_STATUS)
  @Operation(
      summary = SwaggerConstants.Operations.CHANGE_STATUS,
      description = SwaggerConstants.Operations.CHANGE_STATUS_DESCRIPTION,
      requestBody =
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      examples = @ExampleObject(value = SwaggerConstants.Examples.STATUS_REQUEST))))
  @ApiResponse(
      responseCode = SwaggerConstants.Status.OK,
      description = SwaggerConstants.Responses.OK,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              examples = @ExampleObject(value = SwaggerConstants.Examples.VEHICLE)))
  @ApiResponse(
      responseCode = SwaggerConstants.Status.NOT_FOUND,
      description = SwaggerConstants.Responses.NOT_FOUND,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(value = SwaggerConstants.Examples.ERROR_NOT_FOUND)))
  @ApiResponse(
      responseCode = SwaggerConstants.Status.CONFLICT,
      description = SwaggerConstants.Responses.STATUS_CONFLICT,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ErrorResponse.class),
              examples = {
                @ExampleObject(
                    name = SwaggerConstants.ExampleNames.INVALID_TRANSITION,
                    value = SwaggerConstants.Examples.ERROR_INVALID_TRANSITION),
                @ExampleObject(
                    name = SwaggerConstants.ExampleNames.INVALID_ODOMETER,
                    value = SwaggerConstants.Examples.ERROR_INVALID_ODOMETER)
              }))
  @CommonErrorResponses
  public VehicleResponse changeStatus(
      @Parameter(description = SwaggerConstants.Params.VEHICLE_ID) @PathVariable UUID vehicleId,
      @Valid @RequestBody VehicleStatusRequest request) {
    return vehicleService.changeStatus(vehicleId, request);
  }
}
