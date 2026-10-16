package com.fleetcontrol.msmaintenance.controller;

import com.fleetcontrol.msmaintenance.constants.ApiPaths;
import com.fleetcontrol.msmaintenance.constants.SwaggerConstants;
import com.fleetcontrol.msmaintenance.models.dto.request.CreatePlanRequest;
import com.fleetcontrol.msmaintenance.models.dto.response.ErrorResponse;
import com.fleetcontrol.msmaintenance.models.dto.response.PageResponse;
import com.fleetcontrol.msmaintenance.models.dto.response.PlanResponse;
import com.fleetcontrol.msmaintenance.models.enums.MaintenanceType;
import com.fleetcontrol.msmaintenance.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoints for maintenance plans. */
@RestController
@Validated
@Tag(name = SwaggerConstants.Tags.PLANS, description = SwaggerConstants.Tags.PLANS_DESCRIPTION)
@SecurityRequirements({
  @SecurityRequirement(name = SwaggerConstants.Security.BEARER),
  @SecurityRequirement(name = SwaggerConstants.Security.INTERNAL_KEY)
})
@RequiredArgsConstructor
public class PlanController {

  private final PlanService planService;

  /** Creates a maintenance plan for a vehicle. */
  @PostMapping(ApiPaths.Maintenance.PLANS)
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = SwaggerConstants.Operations.CREATE_PLAN,
      description = SwaggerConstants.Operations.CREATE_PLAN_DESCRIPTION)
  @ApiResponses({
    @ApiResponse(
        responseCode = SwaggerConstants.Status.CREATED,
        description = SwaggerConstants.Responses.CREATED),
    @ApiResponse(
        responseCode = SwaggerConstants.Status.BAD_REQUEST,
        description = SwaggerConstants.Responses.BAD_REQUEST,
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = SwaggerConstants.Status.UNAUTHORIZED,
        description = SwaggerConstants.Responses.UNAUTHORIZED,
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = SwaggerConstants.Status.FORBIDDEN,
        description = SwaggerConstants.Responses.FORBIDDEN,
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = SwaggerConstants.Status.NOT_FOUND,
        description = SwaggerConstants.Responses.PLAN_VEHICLE_NOT_FOUND,
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = SwaggerConstants.Status.CONFLICT,
        description = SwaggerConstants.Responses.PLAN_CONFLICT,
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = SwaggerConstants.Status.SERVICE_UNAVAILABLE,
        description = SwaggerConstants.Responses.SERVICE_UNAVAILABLE,
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public PlanResponse create(@Valid @RequestBody CreatePlanRequest request) {
    return planService.create(request);
  }

  /** Lists maintenance plans with optional filters and pagination. */
  @GetMapping(ApiPaths.Maintenance.PLANS)
  @Operation(
      summary = SwaggerConstants.Operations.LIST_PLANS,
      description = SwaggerConstants.Operations.LIST_PLANS_DESCRIPTION)
  @ApiResponses({
    @ApiResponse(
        responseCode = SwaggerConstants.Status.OK,
        description = SwaggerConstants.Responses.OK),
    @ApiResponse(
        responseCode = SwaggerConstants.Status.BAD_REQUEST,
        description = SwaggerConstants.Responses.BAD_REQUEST,
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = SwaggerConstants.Status.UNAUTHORIZED,
        description = SwaggerConstants.Responses.UNAUTHORIZED,
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = SwaggerConstants.Status.FORBIDDEN,
        description = SwaggerConstants.Responses.FORBIDDEN,
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public PageResponse<PlanResponse> list(
      @Parameter(description = SwaggerConstants.Params.VEHICLE)
          @RequestParam(name = "vehicle", required = false)
          UUID vehicleId,
      @Parameter(description = SwaggerConstants.Params.PLAN_TYPE) @RequestParam(required = false)
          MaintenanceType type,
      @Parameter(description = SwaggerConstants.Params.PLAN_ACTIVE) @RequestParam(required = false)
          Boolean active,
      @Parameter(description = SwaggerConstants.Params.PAGE)
          @RequestParam(defaultValue = "0")
          @Min(0)
          int page,
      @Parameter(description = SwaggerConstants.Params.SIZE)
          @RequestParam(defaultValue = "20")
          @Min(1)
          @Max(100)
          int size) {
    return planService.list(vehicleId, type, active, page, size);
  }
}
