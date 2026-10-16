package com.fleetcontrol.msdrivers.api;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fleetcontrol.msdrivers.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/** The OpenAPI document must describe every endpoint with its responses and examples. */
class SwaggerDocsTest extends AbstractIntegrationTest {

  private static final String API_DOCS = "/v3/api-docs";
  private static final String UPDATE_OPERATION = "$.paths['/api/drivers/{driverId}'].put";

  @Test
  void apiDocs_arePublicAndListEveryEndpoint() throws Exception {
    mockMvc
        .perform(get(API_DOCS))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.info.title").value("FleetControl - ms-drivers"))
        .andExpect(jsonPath("$.paths['/api/drivers'].get").exists())
        .andExpect(jsonPath("$.paths['/api/drivers'].post").exists())
        .andExpect(jsonPath("$.paths['/api/drivers/{driverId}'].get").exists())
        .andExpect(jsonPath(UPDATE_OPERATION).exists());
  }

  @Test
  void updateOperation_documentsSpecificAndCommonResponses() throws Exception {
    mockMvc
        .perform(get(API_DOCS))
        .andExpect(jsonPath(UPDATE_OPERATION + ".responses['200']").exists())
        .andExpect(jsonPath(UPDATE_OPERATION + ".responses['400']").exists())
        .andExpect(jsonPath(UPDATE_OPERATION + ".responses['401']").exists())
        .andExpect(jsonPath(UPDATE_OPERATION + ".responses['403']").exists())
        .andExpect(jsonPath(UPDATE_OPERATION + ".responses['404']").exists())
        .andExpect(jsonPath(UPDATE_OPERATION + ".responses['409']").exists());
  }

  @Test
  void documentation_includesExamplesAndSecuritySchemes() throws Exception {
    mockMvc
        .perform(get(API_DOCS))
        .andExpect(content().string(containsString("DRIVER_ALREADY_EXISTS")))
        .andExpect(content().string(containsString("B-5120377")))
        .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
        .andExpect(
            jsonPath("$.components.securitySchemes.internalKey.name").value("X-Internal-Key"));
  }
}
