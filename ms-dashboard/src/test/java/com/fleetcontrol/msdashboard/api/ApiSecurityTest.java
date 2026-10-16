package com.fleetcontrol.msdashboard.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fleetcontrol.msdashboard.support.AbstractApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

/** Authentication and role rules of the dashboard endpoints. */
class ApiSecurityTest extends AbstractApiTest {

  private static final String DASHBOARD = "/api/dashboard";
  private static final String TIMESERIES = "/api/dashboard/timeseries";
  private static final String RANKING = "/api/dashboard/vehicles/ranking";
  private static final String INTERNAL_KEY_HEADER = "X-Internal-Key";

  @Test
  void request_withoutCredentials_isUnauthorized() throws Exception {
    mockMvc
        .perform(get(DASHBOARD))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  void everyEndpoint_withoutCredentials_isUnauthorized() throws Exception {
    mockMvc.perform(get(TIMESERIES)).andExpect(status().isUnauthorized());
    mockMvc.perform(get(RANKING)).andExpect(status().isUnauthorized());
  }

  @Test
  void request_withMalformedToken_isUnauthorized() throws Exception {
    mockMvc
        .perform(get(DASHBOARD).header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
  }

  @Test
  void request_withExpiredToken_returnsTokenExpired() throws Exception {
    mockMvc
        .perform(get(DASHBOARD).header(HttpHeaders.AUTHORIZATION, expiredBearer("MANAGER")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("TOKEN_EXPIRED"));
  }

  @Test
  void request_withUnknownRole_isForbidden() throws Exception {
    mockMvc
        .perform(get(DASHBOARD).header(HttpHeaders.AUTHORIZATION, bearer("DRIVER")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("FORBIDDEN"));
  }

  @Test
  void request_withManagerToken_isAllowed() throws Exception {
    mockMvc
        .perform(get(DASHBOARD).header(HttpHeaders.AUTHORIZATION, bearer("MANAGER")))
        .andExpect(status().isOk());
  }

  @Test
  void request_withAdminToken_isAllowed() throws Exception {
    mockMvc
        .perform(get(DASHBOARD).header(HttpHeaders.AUTHORIZATION, bearer("ADMIN")))
        .andExpect(status().isOk());
  }

  @Test
  void request_withInternalKey_isForbidden() throws Exception {
    mockMvc
        .perform(get(DASHBOARD).header(INTERNAL_KEY_HEADER, INTERNAL_KEY))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("FORBIDDEN"));
  }

  @Test
  void request_withWrongInternalKey_isUnauthorized() throws Exception {
    mockMvc
        .perform(get(DASHBOARD).header(INTERNAL_KEY_HEADER, "wrong"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
  }

  @Test
  void health_isPublic() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }
}
