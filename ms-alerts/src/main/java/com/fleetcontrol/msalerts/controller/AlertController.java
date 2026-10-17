package com.fleetcontrol.msalerts.controller;

import com.fleetcontrol.msalerts.constants.ApiPaths;
import com.fleetcontrol.msalerts.constants.BusinessConstants;
import com.fleetcontrol.msalerts.constants.SwaggerConstants;
import com.fleetcontrol.msalerts.documentation.CommonErrorResponses;
import com.fleetcontrol.msalerts.models.dto.request.AcknowledgeRequest;
import com.fleetcontrol.msalerts.models.dto.response.AlertResponse;
import com.fleetcontrol.msalerts.models.dto.response.ErrorResponse;
import com.fleetcontrol.msalerts.models.dto.response.EvaluationResponse;
import com.fleetcontrol.msalerts.models.dto.response.PageResponse;
import com.fleetcontrol.msalerts.models.dto.response.StatsResponse;
import com.fleetcontrol.msalerts.models.enums.AlertSeverity;
import com.fleetcontrol.msalerts.models.enums.AlertStatus;
import com.fleetcontrol.msalerts.models.enums.AlertType;
import com.fleetcontrol.msalerts.models.enums.Granularity;
import com.fleetcontrol.msalerts.service.AlertService;
import com.fleetcontrol.msalerts.service.StatsService;
import com.fleetcontrol.msalerts.service.evaluation.AlertEvaluator;
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
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoints to list, acknowledge and measure alerts, and to launch an evaluation. */
@RestController
@RequestMapping(ApiPaths.Alerts.BASE)
@Validated
@Tag(name = SwaggerConstants.Tags.ALERTS, description = SwaggerConstants.Tags.ALERTS_DESCRIPTION)
@SecurityRequirement(name = SwaggerConstants.Security.BEARER)
@RequiredArgsConstructor
public class AlertController {

  private final AlertService alertService;
  private final StatsService statsService;
  private final AlertEvaluator evaluator;

  /** Lists alerts with optional filters and pagination. */
  @GetMapping
  @SecurityRequirements({
    @SecurityRequirement(name = SwaggerConstants.Security.BEARER),
    @SecurityRequirement(name = SwaggerConstants.Security.INTERNAL_KEY)
  })
  @Operation(
      summary = SwaggerConstants.Operations.LIST,
      description = SwaggerConstants.Operations.LIST_DESCRIPTION)
  @ApiResponse(
      responseCode = SwaggerConstants.Status.OK,
      description = SwaggerConstants.Responses.OK,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              examples = @ExampleObject(value = SwaggerConstants.Examples.ALERT_PAGE)))
  @CommonErrorResponses
  public PageResponse<AlertResponse> list(
      @Parameter(description = SwaggerConstants.Params.STATUS) @RequestParam(required = false)
          AlertStatus status,
      @Parameter(description = SwaggerConstants.Params.SEVERITY) @RequestParam(required = false)
          AlertSeverity severity,
      @Parameter(description = SwaggerConstants.Params.TYPE) @RequestParam(required = false)
          AlertType type,
      @Parameter(description = SwaggerConstants.Params.VEHICLE)
          @RequestParam(name = "vehicle", required = false)
          UUID vehicleId,
      @Parameter(description = SwaggerConstants.Params.PAGE)
          @RequestParam(defaultValue = BusinessConstants.Pagination.DEFAULT_PAGE)
          @Min(BusinessConstants.Pagination.MIN_PAGE)
          int page,
      @Parameter(description = SwaggerConstants.Params.SIZE)
          @RequestParam(defaultValue = BusinessConstants.Pagination.DEFAULT_SIZE)
          @Min(BusinessConstants.Pagination.MIN_SIZE)
          @Max(BusinessConstants.Pagination.MAX_SIZE)
          int size) {
    return alertService.list(status, severity, type, vehicleId, page, size);
  }

  /** Marks an open alert as acknowledged by the authenticated user. */
  @PostMapping(ApiPaths.Alerts.ACKNOWLEDGE)
  @Operation(
      summary = SwaggerConstants.Operations.ACKNOWLEDGE,
      description = SwaggerConstants.Operations.ACKNOWLEDGE_DESCRIPTION,
      requestBody =
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              required = false,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      examples =
                          @ExampleObject(value = SwaggerConstants.Examples.ACKNOWLEDGE_REQUEST))))
  @ApiResponse(
      responseCode = SwaggerConstants.Status.OK,
      description = SwaggerConstants.Responses.OK,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              examples = @ExampleObject(value = SwaggerConstants.Examples.ALERT)))
  @ApiResponse(
      responseCode = SwaggerConstants.Status.NOT_FOUND,
      description = SwaggerConstants.Responses.ALERT_NOT_FOUND,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(value = SwaggerConstants.Examples.ERROR_ALERT_NOT_FOUND)))
  @ApiResponse(
      responseCode = SwaggerConstants.Status.CONFLICT,
      description = SwaggerConstants.Responses.INVALID_STATE,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(value = SwaggerConstants.Examples.ERROR_INVALID_STATE)))
  @CommonErrorResponses
  public AlertResponse acknowledge(
      @Parameter(description = SwaggerConstants.Params.ALERT_ID) @PathVariable UUID alertId,
      @Valid @RequestBody(required = false) AcknowledgeRequest request,
      Authentication authentication) {
    return alertService.acknowledge(alertId, request, authentication.getName());
  }

  /** Runs an evaluation right now. */
  @PostMapping(ApiPaths.Alerts.EVALUATE)
  @Operation(
      summary = SwaggerConstants.Operations.EVALUATE,
      description = SwaggerConstants.Operations.EVALUATE_DESCRIPTION)
  @ApiResponse(
      responseCode = SwaggerConstants.Status.OK,
      description = SwaggerConstants.Responses.OK,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              examples = @ExampleObject(value = SwaggerConstants.Examples.EVALUATION)))
  @CommonErrorResponses
  public EvaluationResponse evaluate() {
    return evaluator.evaluate();
  }

  /** Alerts created over time and alerts that are open now. */
  @GetMapping(ApiPaths.Alerts.STATS)
  @SecurityRequirements({
    @SecurityRequirement(name = SwaggerConstants.Security.BEARER),
    @SecurityRequirement(name = SwaggerConstants.Security.INTERNAL_KEY)
  })
  @Operation(
      summary = SwaggerConstants.Operations.STATS,
      description = SwaggerConstants.Operations.STATS_DESCRIPTION)
  @ApiResponse(
      responseCode = SwaggerConstants.Status.OK,
      description = SwaggerConstants.Responses.OK,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              examples = @ExampleObject(value = SwaggerConstants.Examples.STATS)))
  @CommonErrorResponses
  public StatsResponse stats(
      @Parameter(
              description = SwaggerConstants.Params.FROM,
              example = SwaggerConstants.Params.EXAMPLE_FROM)
          @RequestParam
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate from,
      @Parameter(
              description = SwaggerConstants.Params.TO,
              example = SwaggerConstants.Params.EXAMPLE_TO)
          @RequestParam
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate to,
      @Parameter(description = SwaggerConstants.Params.GRANULARITY)
          @RequestParam(defaultValue = BusinessConstants.Stats.DEFAULT_GRANULARITY)
          Granularity granularity) {
    return statsService.stats(from, to, granularity);
  }
}
