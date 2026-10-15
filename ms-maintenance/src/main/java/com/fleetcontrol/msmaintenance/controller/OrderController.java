package com.fleetcontrol.msmaintenance.controller;

import com.fleetcontrol.msmaintenance.constants.ApiPaths;
import com.fleetcontrol.msmaintenance.constants.SwaggerConstants;
import com.fleetcontrol.msmaintenance.models.dto.request.CompleteOrderRequest;
import com.fleetcontrol.msmaintenance.models.dto.response.CompleteOrderResponse;
import com.fleetcontrol.msmaintenance.models.dto.response.ErrorResponse;
import com.fleetcontrol.msmaintenance.models.dto.response.OrderResponse;
import com.fleetcontrol.msmaintenance.models.dto.response.PageResponse;
import com.fleetcontrol.msmaintenance.models.enums.MaintenanceOrderStatus;
import com.fleetcontrol.msmaintenance.models.enums.MaintenanceType;
import com.fleetcontrol.msmaintenance.service.OrderService;
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
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoints for maintenance orders. */
@RestController
@Validated
@Tag(name = SwaggerConstants.Tags.ORDERS, description = SwaggerConstants.Tags.ORDERS_DESCRIPTION)
@SecurityRequirements({
  @SecurityRequirement(name = SwaggerConstants.Security.BEARER),
  @SecurityRequirement(name = SwaggerConstants.Security.INTERNAL_KEY)
})
@RequiredArgsConstructor
public class OrderController {

  private final OrderService orderService;

  /** Lists maintenance orders by scheduled date, with optional filters and pagination. */
  @GetMapping(ApiPaths.Maintenance.ORDERS)
  @Operation(
      summary = SwaggerConstants.Operations.LIST_ORDERS,
      description = SwaggerConstants.Operations.LIST_ORDERS_DESCRIPTION)
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
  public PageResponse<OrderResponse> list(
      @Parameter(description = SwaggerConstants.Params.VEHICLE)
          @RequestParam(name = "vehicle", required = false)
          UUID vehicleId,
      @Parameter(description = SwaggerConstants.Params.ORDER_STATUS) @RequestParam(required = false)
          MaintenanceOrderStatus status,
      @Parameter(description = SwaggerConstants.Params.PLAN_TYPE) @RequestParam(required = false)
          MaintenanceType type,
      @Parameter(description = SwaggerConstants.Params.ORDER_DUE_BEFORE)
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate dueBefore,
      @Parameter(description = SwaggerConstants.Params.PAGE)
          @RequestParam(defaultValue = "0")
          @Min(0)
          int page,
      @Parameter(description = SwaggerConstants.Params.SIZE)
          @RequestParam(defaultValue = "20")
          @Min(1)
          @Max(100)
          int size) {
    return orderService.list(vehicleId, status, type, dueBefore, page, size);
  }

  /** Starts a pending order and puts its vehicle in maintenance. */
  @PostMapping(ApiPaths.Maintenance.ORDER_START)
  @Operation(
      summary = SwaggerConstants.Operations.START_ORDER,
      description = SwaggerConstants.Operations.START_ORDER_DESCRIPTION)
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
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = SwaggerConstants.Status.NOT_FOUND,
        description = SwaggerConstants.Responses.ORDER_NOT_FOUND,
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = SwaggerConstants.Status.CONFLICT,
        description = SwaggerConstants.Responses.START_CONFLICT,
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = SwaggerConstants.Status.SERVICE_UNAVAILABLE,
        description = SwaggerConstants.Responses.SERVICE_UNAVAILABLE,
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public OrderResponse start(
      @Parameter(description = SwaggerConstants.Params.ORDER_ID) @PathVariable UUID orderId) {
    return orderService.start(orderId);
  }

  /** Completes an order in progress and recalculates its plan. */
  @PostMapping(ApiPaths.Maintenance.ORDER_COMPLETE)
  @Operation(
      summary = SwaggerConstants.Operations.COMPLETE_ORDER,
      description = SwaggerConstants.Operations.COMPLETE_ORDER_DESCRIPTION)
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
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = SwaggerConstants.Status.NOT_FOUND,
        description = SwaggerConstants.Responses.ORDER_NOT_FOUND,
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = SwaggerConstants.Status.CONFLICT,
        description = SwaggerConstants.Responses.COMPLETE_CONFLICT,
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = SwaggerConstants.Status.SERVICE_UNAVAILABLE,
        description = SwaggerConstants.Responses.SERVICE_UNAVAILABLE,
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public CompleteOrderResponse complete(
      @Parameter(description = SwaggerConstants.Params.ORDER_ID) @PathVariable UUID orderId,
      @Valid @RequestBody CompleteOrderRequest request) {
    return orderService.complete(orderId, request);
  }
}
