package com.fleetcontrol.msdrivers.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fleetcontrol.msdrivers.models.entities.Driver;
import com.fleetcontrol.msdrivers.support.AbstractIntegrationTest;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/** Authentication and role rules of the driver endpoints. */
class ApiSecurityTest extends AbstractIntegrationTest {

  private static final String DRIVERS = "/api/drivers";
  private static final String INTERNAL_KEY_HEADER = "X-Internal-Key";
  private static final String EMPTY_BODY = "{}";

  @Test
  void request_withoutCredentials_isUnauthorized() throws Exception {
    mockMvc
        .perform(get(DRIVERS))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  void createDriver_withoutCredentials_isUnauthorized() throws Exception {
    mockMvc
        .perform(post(DRIVERS).contentType(MediaType.APPLICATION_JSON).content(EMPTY_BODY))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
  }

  @Test
  void getDriver_withoutCredentials_isUnauthorized() throws Exception {
    Driver driver = saveDriver("B-1000001", LocalDate.now().plusYears(2));

    mockMvc
        .perform(get(DRIVERS + "/{id}", driver.getId()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
  }

  @Test
  void request_withMalformedToken_isUnauthorized() throws Exception {
    mockMvc
        .perform(get(DRIVERS).header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
  }

  @Test
  void request_withExpiredToken_returnsTokenExpired() throws Exception {
    mockMvc
        .perform(
            get(DRIVERS).header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken("MANAGER")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("TOKEN_EXPIRED"));
  }

  @Test
  void request_withUnknownRole_isForbidden() throws Exception {
    mockMvc
        .perform(get(DRIVERS).header(HttpHeaders.AUTHORIZATION, bearer("DRIVER")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("FORBIDDEN"));
  }

  @Test
  void updateDriver_withUnknownRole_isForbidden() throws Exception {
    mockMvc
        .perform(
            put(DRIVERS + "/{id}", UUID.randomUUID())
                .header(HttpHeaders.AUTHORIZATION, bearer("DRIVER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(EMPTY_BODY))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("FORBIDDEN"));
  }

  @Test
  void request_withManagerToken_isAllowed() throws Exception {
    mockMvc
        .perform(get(DRIVERS).header(HttpHeaders.AUTHORIZATION, bearer("MANAGER")))
        .andExpect(status().isOk());
  }

  @Test
  void request_withAdminToken_isAllowed() throws Exception {
    mockMvc
        .perform(get(DRIVERS).header(HttpHeaders.AUTHORIZATION, bearer("ADMIN")))
        .andExpect(status().isOk());
  }

  @Test
  void request_withInternalKey_isAllowed() throws Exception {
    Driver driver = saveDriver("B-1000002", LocalDate.now().plusYears(2));

    mockMvc
        .perform(get(DRIVERS + "/{id}", driver.getId()).header(INTERNAL_KEY_HEADER, INTERNAL_KEY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.licenseNumber").value("B-1000002"));
  }

  @Test
  void request_withWrongInternalKey_isUnauthorized() throws Exception {
    mockMvc
        .perform(get(DRIVERS).header(INTERNAL_KEY_HEADER, "wrong"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
  }

  @Test
  void health_isPublic() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }
}
