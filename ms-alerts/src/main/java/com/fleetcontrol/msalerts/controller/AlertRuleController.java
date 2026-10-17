package com.fleetcontrol.msalerts.controller;

import com.fleetcontrol.msalerts.constants.ApiPaths;
import com.fleetcontrol.msalerts.constants.SwaggerConstants;
import com.fleetcontrol.msalerts.documentation.CommonErrorResponses;
import com.fleetcontrol.msalerts.models.dto.request.UpdateRuleRequest;
import com.fleetcontrol.msalerts.models.dto.response.ErrorResponse;
import com.fleetcontrol.msalerts.models.dto.response.RuleResponse;
import com.fleetcontrol.msalerts.service.AlertRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoints for the configuration of the alert rules. */
@RestController
@RequestMapping(ApiPaths.Alerts.BASE)
@Tag(name = SwaggerConstants.Tags.RULES, description = SwaggerConstants.Tags.RULES_DESCRIPTION)
@SecurityRequirement(name = SwaggerConstants.Security.BEARER)
@RequiredArgsConstructor
public class AlertRuleController {

  private final AlertRuleService ruleService;

  /** Lists the rules and their configuration. */
  @GetMapping(ApiPaths.Alerts.RULES)
  @Operation(
      summary = SwaggerConstants.Operations.LIST_RULES,
      description = SwaggerConstants.Operations.LIST_RULES_DESCRIPTION)
  @ApiResponse(
      responseCode = SwaggerConstants.Status.OK,
      description = SwaggerConstants.Responses.OK,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              examples = @ExampleObject(value = SwaggerConstants.Examples.RULES)))
  @CommonErrorResponses
  public List<RuleResponse> list() {
    return ruleService.list();
  }

  /** Replaces the configuration of one rule. */
  @PutMapping(ApiPaths.Alerts.RULE_TYPE)
  @Operation(
      summary = SwaggerConstants.Operations.UPDATE_RULE,
      description = SwaggerConstants.Operations.UPDATE_RULE_DESCRIPTION,
      requestBody =
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      examples =
                          @ExampleObject(value = SwaggerConstants.Examples.UPDATE_RULE_REQUEST))))
  @ApiResponse(
      responseCode = SwaggerConstants.Status.OK,
      description = SwaggerConstants.Responses.OK,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              examples = @ExampleObject(value = SwaggerConstants.Examples.RULE)))
  @ApiResponse(
      responseCode = SwaggerConstants.Status.NOT_FOUND,
      description = SwaggerConstants.Responses.RULE_NOT_FOUND,
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(value = SwaggerConstants.Examples.ERROR_RULE_NOT_FOUND)))
  @CommonErrorResponses
  public RuleResponse update(
      @Parameter(description = SwaggerConstants.Params.RULE_TYPE) @PathVariable String type,
      @Valid @RequestBody UpdateRuleRequest request) {
    return ruleService.update(type, request);
  }
}
