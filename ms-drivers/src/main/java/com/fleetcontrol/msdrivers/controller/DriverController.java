package com.fleetcontrol.msdrivers.controller;

import com.fleetcontrol.msdrivers.constants.ApiPaths;
import com.fleetcontrol.msdrivers.constants.BusinessConstants;
import com.fleetcontrol.msdrivers.constants.SwaggerConstants;
import com.fleetcontrol.msdrivers.documentation.CommonErrorResponses;
import com.fleetcontrol.msdrivers.models.dto.request.CreateDriverRequest;
import com.fleetcontrol.msdrivers.models.dto.request.UpdateDriverRequest;
import com.fleetcontrol.msdrivers.models.dto.response.DriverResponse;
import com.fleetcontrol.msdrivers.models.dto.response.DriverSummaryResponse;
import com.fleetcontrol.msdrivers.models.dto.response.ErrorResponse;
import com.fleetcontrol.msdrivers.models.dto.response.PageResponse;
import com.fleetcontrol.msdrivers.models.enums.DriverStatus;
import com.fleetcontrol.msdrivers.service.DriverService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoints for the driver catalogue. */
@RestController
@RequestMapping(ApiPaths.Drivers.BASE)
@Validated
@Tag(name = SwaggerConstants.Tags.DRIVERS, description = SwaggerConstants.Tags.DRIVERS_DESCRIPTION)
@SecurityRequirements({
  @SecurityRequirement(name = SwaggerConstants.Security.BEARER),
  @SecurityRequirement(name = SwaggerConstants.Security.INTERNAL_KEY)
})
@RequiredArgsConstructor
public class DriverController {

  private final DriverService driverService;

  /** Lists drivers with optional filters and pagination. */
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
              examples = @ExampleObject(value = SwaggerConstants.Examples.DRIVER_PAGE)))
  @CommonErrorResponses
  public PageResponse<DriverSummaryResponse> list(
      @Parameter(description = SwaggerConstants.Params.STATUS) @RequestParam(required = false)
          DriverStatus status,
      @Parameter(description = SwaggerConstants.Params.LICENSE_EXPIRING_IN_DAYS)
          @RequestParam(required = false)
          @Min(BusinessConstants.MIN_EXPIRING_DAYS)
          Integer licenseExpiringInDays,
      @Parameter(description = SwaggerConstants.Params.PAGE)
          @RequestParam(defaultValue = BusinessConstants.Pagination.DEFAULT_PAGE)
          @Min(BusinessConstants.Pagination.MIN_PAGE)
          int page,
      @Parameter(description = SwaggerConstants.Params.SIZE)
          @RequestParam(defaultValue = BusinessConstants.Pagination.DEFAULT_SIZE)
          @Min(BusinessConstants.Pagination.MIN_SIZE)
          @Max(BusinessConstants.Pagination.MAX_SIZE)
          int size) {
    return driverService.list(status, licenseExpiringInDays, page, size);
  }

  /** Returns the detailed data of one driver. */
  @GetMapping(ApiPaths.Drivers.DRIVER_ID)
  @Operation(
      summary = SwaggerConstants.Operations.GET,
      description = SwaggerConstants.Operations.GET_DESCRIPTION)
  @ApiResponse(
      responseCode = SwaggerConstants.Status.OK,
      description = SwaggerConstants.Responses.OK,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              examples = @ExampleObject(value = SwaggerConstants.Examples.DRIVER)))
  @ApiResponse(
      responseCode = SwaggerConstants.Status.NOT_FOUND,
      description = SwaggerConstants.Responses.NOT_FOUND,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(value = SwaggerConstants.Examples.ERROR_NOT_FOUND)))
  @CommonErrorResponses
  public DriverResponse get(
      @Parameter(description = SwaggerConstants.Params.DRIVER_ID) @PathVariable UUID driverId) {
    return driverService.getById(driverId);
  }

  /** Registers a new driver. */
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
              examples = @ExampleObject(value = SwaggerConstants.Examples.DRIVER)))
  @ApiResponse(
      responseCode = SwaggerConstants.Status.CONFLICT,
      description = SwaggerConstants.Responses.LICENSE_CONFLICT,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(value = SwaggerConstants.Examples.ERROR_LICENSE_EXISTS)))
  @CommonErrorResponses
  public DriverResponse create(@Valid @RequestBody CreateDriverRequest request) {
    return driverService.create(request);
  }

  /** Updates every data of a driver, including its status and license. */
  @PutMapping(ApiPaths.Drivers.DRIVER_ID)
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
              examples = @ExampleObject(value = SwaggerConstants.Examples.DRIVER)))
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
      description = SwaggerConstants.Responses.LICENSE_CONFLICT,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(value = SwaggerConstants.Examples.ERROR_LICENSE_EXISTS)))
  @CommonErrorResponses
  public DriverResponse update(
      @Parameter(description = SwaggerConstants.Params.DRIVER_ID) @PathVariable UUID driverId,
      @Valid @RequestBody UpdateDriverRequest request) {
    return driverService.update(driverId, request);
  }
}
