package com.fleetcontrol.msalerts.api;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fleetcontrol.msalerts.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/** The OpenAPI document must describe every endpoint with its responses and examples. */
class SwaggerDocsTest extends AbstractIntegrationTest {

  private static final String API_DOCS = "/v3/api-docs";
  private static final String ACKNOWLEDGE =
      "$.paths['/api/alerts/{alertId}/acknowledge'].post";
  private static final String UPDATE_RULE = "$.paths['/api/alerts/rules/{type}'].put";

  @Test
  void apiDocs_arePublicAndListEveryEndpoint() throws Exception {
    mockMvc
        .perform(get(API_DOCS))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.info.title").value("FleetControl - ms-alerts"))
        .andExpect(jsonPath("$.paths['/api/alerts'].get").exists())
        .andExpect(jsonPath(ACKNOWLEDGE).exists())
        .andExpect(jsonPath("$.paths['/api/alerts/rules'].get").exists())
        .andExpect(jsonPath(UPDATE_RULE).exists())
        .andExpect(jsonPath("$.paths['/api/alerts/evaluate'].post").exists())
        .andExpect(jsonPath("$.paths['/api/alerts/stats'].get").exists());
  }

  @Test
  void acknowledgeOperation_documentsSpecificAndCommonResponses() throws Exception {
    mockMvc
        .perform(get(API_DOCS))
        .andExpect(jsonPath(ACKNOWLEDGE + ".responses['200']").exists())
        .andExpect(jsonPath(ACKNOWLEDGE + ".responses['400']").exists())
        .andExpect(jsonPath(ACKNOWLEDGE + ".responses['401']").exists())
        .andExpect(jsonPath(ACKNOWLEDGE + ".responses['403']").exists())
        .andExpect(jsonPath(ACKNOWLEDGE + ".responses['404']").exists())
        .andExpect(jsonPath(ACKNOWLEDGE + ".responses['409']").exists());
  }

  @Test
  void updateRuleOperation_documentsNotFoundAndValidationErrors() throws Exception {
    mockMvc
        .perform(get(API_DOCS))
        .andExpect(jsonPath(UPDATE_RULE + ".responses['200']").exists())
        .andExpect(jsonPath(UPDATE_RULE + ".responses['400']").exists())
        .andExpect(jsonPath(UPDATE_RULE + ".responses['404']").exists());
  }

  @Test
  void documentation_includesExamplesAndSecuritySchemes() throws Exception {
    mockMvc
        .perform(get(API_DOCS))
        .andExpect(content().string(containsString("INVALID_ALERT_STATE")))
        .andExpect(content().string(containsString("MAINTENANCE_OVERDUE")))
        .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
        .andExpect(
            jsonPath("$.components.securitySchemes.internalKey.name").value("X-Internal-Key"));
  }
}
