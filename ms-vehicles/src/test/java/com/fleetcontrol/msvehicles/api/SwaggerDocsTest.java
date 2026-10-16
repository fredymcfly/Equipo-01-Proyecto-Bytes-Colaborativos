package com.fleetcontrol.msvehicles.api;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fleetcontrol.msvehicles.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/** The OpenAPI document must describe every endpoint with its responses and examples. */
class SwaggerDocsTest extends AbstractIntegrationTest {

  private static final String API_DOCS = "/v3/api-docs";
  private static final String STATUS_OPERATION =
      "$.paths['/api/vehicles/{vehicleId}/status'].patch";

  @Test
  void apiDocs_arePublicAndListEveryEndpoint() throws Exception {
    mockMvc
        .perform(get(API_DOCS))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.info.title").value("FleetControl - ms-vehicles"))
        .andExpect(jsonPath("$.paths['/api/vehicles'].get").exists())
        .andExpect(jsonPath("$.paths['/api/vehicles'].post").exists())
        .andExpect(jsonPath("$.paths['/api/vehicles/summary'].get").exists())
        .andExpect(jsonPath("$.paths['/api/vehicles/{vehicleId}'].get").exists())
        .andExpect(jsonPath("$.paths['/api/vehicles/{vehicleId}'].put").exists())
        .andExpect(jsonPath(STATUS_OPERATION).exists());
  }

  @Test
  void statusOperation_documentsSpecificAndCommonResponses() throws Exception {
    mockMvc
        .perform(get(API_DOCS))
        .andExpect(jsonPath(STATUS_OPERATION + ".responses['200']").exists())
        .andExpect(jsonPath(STATUS_OPERATION + ".responses['400']").exists())
        .andExpect(jsonPath(STATUS_OPERATION + ".responses['401']").exists())
        .andExpect(jsonPath(STATUS_OPERATION + ".responses['403']").exists())
        .andExpect(jsonPath(STATUS_OPERATION + ".responses['404']").exists())
        .andExpect(jsonPath(STATUS_OPERATION + ".responses['409']").exists());
  }

  @Test
  void documentation_includesExamplesAndSecuritySchemes() throws Exception {
    mockMvc
        .perform(get(API_DOCS))
        .andExpect(content().string(containsString("INVALID_STATUS_TRANSITION")))
        .andExpect(content().string(containsString("9012-PQR")))
        .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
        .andExpect(
            jsonPath("$.components.securitySchemes.internalKey.name").value("X-Internal-Key"));
  }
}
