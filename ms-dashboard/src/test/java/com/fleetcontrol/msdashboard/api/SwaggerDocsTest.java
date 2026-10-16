package com.fleetcontrol.msdashboard.api;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fleetcontrol.msdashboard.support.AbstractApiTest;
import org.junit.jupiter.api.Test;

/** The OpenAPI document must describe every endpoint with its responses and examples. */
class SwaggerDocsTest extends AbstractApiTest {

  private static final String API_DOCS = "/v3/api-docs";
  private static final String RANKING_OPERATION = "$.paths['/api/dashboard/vehicles/ranking'].get";

  @Test
  void apiDocs_arePublicAndListEveryEndpoint() throws Exception {
    mockMvc
        .perform(get(API_DOCS))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.info.title").value("FleetControl - ms-dashboard"))
        .andExpect(jsonPath("$.paths['/api/dashboard'].get").exists())
        .andExpect(jsonPath("$.paths['/api/dashboard/timeseries'].get").exists())
        .andExpect(jsonPath(RANKING_OPERATION).exists());
  }

  @Test
  void rankingOperation_documentsSpecificAndCommonResponses() throws Exception {
    mockMvc
        .perform(get(API_DOCS))
        .andExpect(jsonPath(RANKING_OPERATION + ".responses['200']").exists())
        .andExpect(jsonPath(RANKING_OPERATION + ".responses['400']").exists())
        .andExpect(jsonPath(RANKING_OPERATION + ".responses['401']").exists())
        .andExpect(jsonPath(RANKING_OPERATION + ".responses['403']").exists())
        .andExpect(jsonPath(RANKING_OPERATION + ".responses['503']").exists());
  }

  @Test
  void documentation_includesExamplesAndSecurityScheme() throws Exception {
    mockMvc
        .perform(get(API_DOCS))
        .andExpect(content().string(containsString("4821-KDF")))
        .andExpect(content().string(containsString("SERVICE_UNAVAILABLE")))
        .andExpect(content().string(containsString("DISTANCE_KM")))
        .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
  }
}
