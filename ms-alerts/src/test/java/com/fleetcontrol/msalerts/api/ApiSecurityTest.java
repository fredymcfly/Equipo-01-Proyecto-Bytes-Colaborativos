package com.fleetcontrol.msalerts.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fleetcontrol.msalerts.support.AbstractIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/** Authentication and role rules of the alert endpoints. */
class ApiSecurityTest extends AbstractIntegrationTest {

  private static final String ALERTS = "/api/alerts";
  private static final String STATS = ALERTS + "/stats?from=2026-09-01&to=2026-09-30";
  private static final String RULES = ALERTS + "/rules";
  private static final String EVALUATE = ALERTS + "/evaluate";
  private static final String RULE_BODY =
      """
      { "enabled": true, "severity": "WARNING", "params": { "daysBefore": 7 } }
      """;

  @Test
  void request_withoutCredentials_isUnauthorized() throws Exception {
    mockMvc
        .perform(get(ALERTS))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  void request_withMalformedToken_isUnauthorized() throws Exception {
    mockMvc
        .perform(get(ALERTS).header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
  }

  @Test
  void request_withExpiredToken_returnsTokenExpired() throws Exception {
    mockMvc
        .perform(get(ALERTS).header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken("ADMIN")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("TOKEN_EXPIRED"));
  }

  @Test
  void request_withWrongInternalKey_isUnauthorized() throws Exception {
    mockMvc
        .perform(get(ALERTS).header(INTERNAL_KEY_HEADER, "wrong"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
  }

  @Test
  void request_withUnknownRole_isForbidden() throws Exception {
    mockMvc
        .perform(get(ALERTS).header(HttpHeaders.AUTHORIZATION, bearer("DRIVER")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("FORBIDDEN"));
  }

  @Test
  void list_isAllowedForManagerAdminAndInternalCalls() throws Exception {
    mockMvc
        .perform(get(ALERTS).header(HttpHeaders.AUTHORIZATION, bearer("MANAGER")))
        .andExpect(status().isOk());
    mockMvc
        .perform(get(ALERTS).header(HttpHeaders.AUTHORIZATION, bearer("ADMIN")))
        .andExpect(status().isOk());
    mockMvc
        .perform(get(ALERTS).header(INTERNAL_KEY_HEADER, INTERNAL_KEY))
        .andExpect(status().isOk());
  }

  @Test
  void stats_isAllowedForManagerAdminAndInternalCalls() throws Exception {
    mockMvc
        .perform(get(STATS).header(HttpHeaders.AUTHORIZATION, bearer("MANAGER")))
        .andExpect(status().isOk());
    mockMvc
        .perform(get(STATS).header(HttpHeaders.AUTHORIZATION, bearer("ADMIN")))
        .andExpect(status().isOk());
    mockMvc
        .perform(get(STATS).header(INTERNAL_KEY_HEADER, INTERNAL_KEY))
        .andExpect(status().isOk());
  }

  @Test
  void acknowledge_withInternalKey_isForbidden() throws Exception {
    mockMvc
        .perform(
            post(ALERTS + "/{id}/acknowledge", UUID.randomUUID())
                .header(INTERNAL_KEY_HEADER, INTERNAL_KEY))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("FORBIDDEN"));
  }

  @Test
  void listRules_isAllowedForManagerButNotForInternalCalls() throws Exception {
    mockMvc
        .perform(get(RULES).header(HttpHeaders.AUTHORIZATION, bearer("MANAGER")))
        .andExpect(status().isOk());
    mockMvc
        .perform(get(RULES).header(INTERNAL_KEY_HEADER, INTERNAL_KEY))
        .andExpect(status().isForbidden());
  }

  @Test
  void updateRule_isOnlyAllowedForAdmin() throws Exception {
    mockMvc
        .perform(
            put(RULES + "/MAINTENANCE_DUE")
                .header(HttpHeaders.AUTHORIZATION, bearer("MANAGER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(RULE_BODY))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    mockMvc
        .perform(
            put(RULES + "/MAINTENANCE_DUE")
                .header(INTERNAL_KEY_HEADER, INTERNAL_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(RULE_BODY))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            put(RULES + "/MAINTENANCE_DUE")
                .header(HttpHeaders.AUTHORIZATION, bearer("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(RULE_BODY))
        .andExpect(status().isOk());
  }

  @Test
  void evaluate_isOnlyAllowedForAdmin() throws Exception {
    mockMvc
        .perform(post(EVALUATE).header(HttpHeaders.AUTHORIZATION, bearer("MANAGER")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    mockMvc
        .perform(post(EVALUATE).header(INTERNAL_KEY_HEADER, INTERNAL_KEY))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(post(EVALUATE).header(HttpHeaders.AUTHORIZATION, bearer("ADMIN")))
        .andExpect(status().isOk());
  }

  @Test
  void health_isPublic() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }
}
