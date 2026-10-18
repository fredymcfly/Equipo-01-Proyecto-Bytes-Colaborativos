package com.fleetcontrol.msfuel.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fleetcontrol.msfuel.config.SecurityConfig;
import com.fleetcontrol.msfuel.config.SecurityErrorHandler;
import com.fleetcontrol.msfuel.service.FuelService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Tests of who can call the endpoints: JWT roles and the internal key of other services. */
@WebMvcTest(FuelController.class)
@Import({SecurityConfig.class, SecurityErrorHandler.class})
@TestPropertySource(
    properties = {
      "internal.api-key=test-internal-key",
      "fleet.jwt.secret=test-secret-for-integration-tests-0123456789"
    })
class FuelSecurityTest {

  private static final String REFUELS = "/api/fuel/refuels";
  private static final String KEY_HEADER = "X-Internal-Key";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private FuelService fuelService;

  @Test
  void withoutCredentials_answers401Unauthorized() throws Exception {
    mockMvc
        .perform(get(REFUELS))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("WWW-Authenticate", "Bearer"))
        .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
  }

  @Test
  @WithMockUser(roles = "USER")
  void withAnotherRole_answers403Forbidden() throws Exception {
    mockMvc
        .perform(get(REFUELS))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("FORBIDDEN"));
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void asManager_canQuery() throws Exception {
    mockMvc.perform(get(REFUELS)).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void asAdmin_canQuery() throws Exception {
    mockMvc.perform(get(REFUELS)).andExpect(status().isOk());
  }

  @Test
  void withTheInternalKey_canQuery() throws Exception {
    mockMvc
        .perform(get(REFUELS).header(KEY_HEADER, "test-internal-key"))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            get("/api/fuel/stats")
                .header(KEY_HEADER, "test-internal-key")
                .param("from", "2026-09-01")
                .param("to", "2026-09-30"))
        .andExpect(status().isOk());
  }

  @Test
  void withWrongInternalKey_answers401() throws Exception {
    mockMvc
        .perform(get(REFUELS).header(KEY_HEADER, "wrong-key"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
  }

  @Test
  void theInternalKey_doesNotAllowToRegisterRefuels() throws Exception {
    mockMvc
        .perform(
            post(REFUELS)
                .header(KEY_HEADER, "test-internal-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void healthAndDocumentation_arePublic() throws Exception {
    for (String path : new String[] {"/actuator/health", "/v3/api-docs", "/swagger-ui.html"}) {
      int status = mockMvc.perform(get(path)).andReturn().getResponse().getStatus();
      assertThat(status).as(path).isNotIn(401, 403);
    }
  }
}
