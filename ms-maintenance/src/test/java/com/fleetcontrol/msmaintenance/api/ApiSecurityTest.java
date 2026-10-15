package com.fleetcontrol.msmaintenance.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fleetcontrol.msmaintenance.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class ApiSecurityTest extends AbstractIntegrationTest {

  private static final String PLANS = "/api/maintenance/plans";

  @Test
  void request_withoutCredentials_isUnauthorized() throws Exception {
    mockMvc
        .perform(get(PLANS))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  void request_withMalformedToken_isUnauthorized() throws Exception {
    mockMvc
        .perform(get(PLANS).header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
  }

  @Test
  void request_withExpiredToken_returnsTokenExpired() throws Exception {
    mockMvc
        .perform(get(PLANS).header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken("MANAGER")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("TOKEN_EXPIRED"));
  }

  @Test
  void request_withDriverRole_isForbidden() throws Exception {
    mockMvc
        .perform(get(PLANS).header(HttpHeaders.AUTHORIZATION, bearer("DRIVER")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("FORBIDDEN"));
  }

  @Test
  void request_withManagerToken_isAllowed() throws Exception {
    mockMvc
        .perform(get(PLANS).header(HttpHeaders.AUTHORIZATION, bearer("MANAGER")))
        .andExpect(status().isOk());
  }

  @Test
  void request_withAdminToken_isAllowed() throws Exception {
    mockMvc
        .perform(get(PLANS).header(HttpHeaders.AUTHORIZATION, bearer("ADMIN")))
        .andExpect(status().isOk());
  }

  @Test
  void request_withInternalKey_isAllowed() throws Exception {
    mockMvc.perform(get(PLANS).header("X-Internal-Key", INTERNAL_KEY)).andExpect(status().isOk());
  }

  @Test
  void request_withWrongInternalKey_isUnauthorized() throws Exception {
    mockMvc
        .perform(get(PLANS).header("X-Internal-Key", "wrong"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
  }

  @Test
  void health_isPublic() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }
}
