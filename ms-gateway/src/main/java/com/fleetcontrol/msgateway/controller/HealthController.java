package com.fleetcontrol.msgateway.controller;

import com.fleetcontrol.msgateway.constants.HealthConstants;
import com.fleetcontrol.msgateway.dto.ErrorResponse;
import com.fleetcontrol.msgateway.dto.HealthResponse;
import com.fleetcontrol.msgateway.service.HealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** Exposes the status of the gateway and of the services behind it. */
@RestController
@RequiredArgsConstructor
@Tag(name = "Health", description = "Gateway and downstream services status")
public class HealthController {

  private final HealthService healthService;

  /** Checks the gateway and every downstream service. */
  @Operation(
      summary = "Gateway and services status",
      description =
          "Queries the /actuator/health of each service with a 2 seconds timeout and reports as"
              + " DOWN the ones that do not answer. It always returns 200.")
  @ApiResponse(
      responseCode = "200",
      description = "Status of the gateway and of the 8 services",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = HealthResponse.class),
              examples =
                  @ExampleObject(
                      name = "One service down",
                      value = HealthConstants.Examples.HEALTH)))
  @ApiResponse(
      responseCode = "429",
      description = "More than the allowed requests per minute from the same IP",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ErrorResponse.class),
              examples =
                  @ExampleObject(
                      name = "Rate limit exceeded",
                      value = HealthConstants.Examples.RATE_LIMIT_EXCEEDED)))
  @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<HealthResponse> health() {
    return healthService.check();
  }
}
