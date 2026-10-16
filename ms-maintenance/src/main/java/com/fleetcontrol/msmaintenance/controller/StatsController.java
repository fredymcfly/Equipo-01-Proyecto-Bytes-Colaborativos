package com.fleetcontrol.msmaintenance.controller;

import com.fleetcontrol.msmaintenance.constants.ApiPaths;
import com.fleetcontrol.msmaintenance.constants.SwaggerConstants;
import com.fleetcontrol.msmaintenance.models.dto.response.ErrorResponse;
import com.fleetcontrol.msmaintenance.models.dto.response.StatsResponse;
import com.fleetcontrol.msmaintenance.models.enums.Granularity;
import com.fleetcontrol.msmaintenance.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoint with the statistics consumed by ms-dashboard. */
@RestController
@Tag(name = SwaggerConstants.Tags.STATS, description = SwaggerConstants.Tags.STATS_DESCRIPTION)
@SecurityRequirements({
  @SecurityRequirement(name = SwaggerConstants.Security.BEARER),
  @SecurityRequirement(name = SwaggerConstants.Security.INTERNAL_KEY)
})
@RequiredArgsConstructor
public class StatsController {

  private final StatsService statsService;

  /** Returns the completed orders of a range as a time series. */
  @GetMapping(ApiPaths.Maintenance.STATS)
  @Operation(
      summary = SwaggerConstants.Operations.STATS,
      description = SwaggerConstants.Operations.STATS_DESCRIPTION)
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
  public StatsResponse stats(
      @Parameter(description = SwaggerConstants.Params.FROM)
          @RequestParam
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate from,
      @Parameter(description = SwaggerConstants.Params.TO)
          @RequestParam
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate to,
      @Parameter(description = SwaggerConstants.Params.GRANULARITY)
          @RequestParam(defaultValue = "DAY")
          Granularity granularity,
      @Parameter(description = SwaggerConstants.Params.VEHICLE)
          @RequestParam(name = "vehicle", required = false)
          UUID vehicleId) {
    return statsService.stats(from, to, granularity, vehicleId);
  }
}
