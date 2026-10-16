package com.fleetcontrol.msdashboard.controller;

import com.fleetcontrol.msdashboard.constants.ApiPaths;
import com.fleetcontrol.msdashboard.constants.BusinessConstants;
import com.fleetcontrol.msdashboard.constants.SwaggerConstants;
import com.fleetcontrol.msdashboard.documentation.CommonErrorResponses;
import com.fleetcontrol.msdashboard.models.dto.request.DashboardQuery;
import com.fleetcontrol.msdashboard.models.dto.request.RankingQuery;
import com.fleetcontrol.msdashboard.models.dto.request.TimeseriesQuery;
import com.fleetcontrol.msdashboard.models.dto.response.DashboardResponse;
import com.fleetcontrol.msdashboard.models.dto.response.ErrorResponse;
import com.fleetcontrol.msdashboard.models.dto.response.RankingResponse;
import com.fleetcontrol.msdashboard.models.dto.response.TimeseriesResponse;
import com.fleetcontrol.msdashboard.models.enums.DashboardMetric;
import com.fleetcontrol.msdashboard.models.enums.Granularity;
import com.fleetcontrol.msdashboard.models.enums.RankingMetric;
import com.fleetcontrol.msdashboard.models.enums.VehicleType;
import com.fleetcontrol.msdashboard.service.DashboardService;
import com.fleetcontrol.msdashboard.service.RankingService;
import com.fleetcontrol.msdashboard.service.TimeseriesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoints of the dashboard. */
@RestController
@RequestMapping(ApiPaths.Dashboard.BASE)
@Validated
@Tag(
    name = SwaggerConstants.Tags.DASHBOARD,
    description = SwaggerConstants.Tags.DASHBOARD_DESCRIPTION)
@SecurityRequirement(name = SwaggerConstants.Security.BEARER)
@RequiredArgsConstructor
public class DashboardController {

  private final DashboardService dashboardService;
  private final TimeseriesService timeseriesService;
  private final RankingService rankingService;

  /** Summary of the fleet for the last days. */
  @GetMapping
  @Operation(
      summary = SwaggerConstants.Operations.OVERVIEW,
      description = SwaggerConstants.Operations.OVERVIEW_DESCRIPTION)
  @ApiResponse(
      responseCode = SwaggerConstants.Status.OK,
      description = SwaggerConstants.Responses.OK,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              examples = {
                @ExampleObject(
                    name = SwaggerConstants.ExampleNames.COMPLETE,
                    value = SwaggerConstants.Examples.OVERVIEW),
                @ExampleObject(
                    name = SwaggerConstants.ExampleNames.PARTIAL,
                    value = SwaggerConstants.Examples.OVERVIEW_PARTIAL)
              }))
  @CommonErrorResponses
  public DashboardResponse overview(
      @Parameter(description = SwaggerConstants.Params.DAYS)
          @RequestParam(defaultValue = BusinessConstants.Days.DEFAULT)
          @Min(BusinessConstants.Days.MIN)
          @Max(BusinessConstants.Days.MAX)
          int days) {
    return dashboardService.overview(new DashboardQuery(days));
  }

  /** Time series of one or more metrics. */
  @GetMapping(ApiPaths.Dashboard.TIMESERIES)
  @Operation(
      summary = SwaggerConstants.Operations.TIMESERIES,
      description = SwaggerConstants.Operations.TIMESERIES_DESCRIPTION)
  @ApiResponse(
      responseCode = SwaggerConstants.Status.OK,
      description = SwaggerConstants.Responses.OK,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              examples = {
                @ExampleObject(
                    name = SwaggerConstants.ExampleNames.COMPLETE,
                    value = SwaggerConstants.Examples.TIMESERIES),
                @ExampleObject(
                    name = SwaggerConstants.ExampleNames.PARTIAL,
                    value = SwaggerConstants.Examples.TIMESERIES_PARTIAL)
              }))
  @CommonErrorResponses
  public TimeseriesResponse timeseries(
      @Parameter(
              description = SwaggerConstants.Params.METRICS,
              example = SwaggerConstants.Params.EXAMPLE_METRICS)
          @RequestParam
          List<DashboardMetric> metrics,
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
          @RequestParam(defaultValue = BusinessConstants.Timeseries.DEFAULT_GRANULARITY)
          Granularity granularity) {
    List<DashboardMetric> distinctMetrics = metrics.stream().distinct().toList();
    return timeseriesService.timeseries(
        new TimeseriesQuery(distinctMetrics, from, to, granularity));
  }

  /** Vehicles sorted by fuel cost or by consumption. */
  @GetMapping(ApiPaths.Dashboard.VEHICLES_RANKING)
  @Operation(
      summary = SwaggerConstants.Operations.RANKING,
      description = SwaggerConstants.Operations.RANKING_DESCRIPTION)
  @ApiResponse(
      responseCode = SwaggerConstants.Status.OK,
      description = SwaggerConstants.Responses.RANKING_OK,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              examples = @ExampleObject(value = SwaggerConstants.Examples.RANKING)))
  @ApiResponse(
      responseCode = SwaggerConstants.Status.SERVICE_UNAVAILABLE,
      description = SwaggerConstants.Responses.SERVICE_UNAVAILABLE,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ErrorResponse.class),
              examples =
                  @ExampleObject(value = SwaggerConstants.Examples.ERROR_SERVICE_UNAVAILABLE)))
  @CommonErrorResponses
  public RankingResponse ranking(
      @Parameter(description = SwaggerConstants.Params.RANKING_METRIC) @RequestParam
          RankingMetric metric,
      @Parameter(description = SwaggerConstants.Params.DAYS)
          @RequestParam(defaultValue = BusinessConstants.Days.DEFAULT)
          @Min(BusinessConstants.Days.MIN)
          @Max(BusinessConstants.Days.MAX)
          int days,
      @Parameter(description = SwaggerConstants.Params.LIMIT)
          @RequestParam(defaultValue = BusinessConstants.Ranking.DEFAULT_LIMIT)
          @Min(BusinessConstants.Ranking.MIN_LIMIT)
          @Max(BusinessConstants.Ranking.MAX_LIMIT)
          int limit,
      @Parameter(description = SwaggerConstants.Params.TYPE) @RequestParam(required = false)
          VehicleType type) {
    return rankingService.ranking(new RankingQuery(metric, days, limit, type));
  }
}
