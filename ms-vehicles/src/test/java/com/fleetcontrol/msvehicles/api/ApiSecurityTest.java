package com.fleetcontrol.msvehicles.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fleetcontrol.msvehicles.models.entities.Vehicle;
import com.fleetcontrol.msvehicles.models.enums.VehicleStatus;
import com.fleetcontrol.msvehicles.models.enums.VehicleType;
import com.fleetcontrol.msvehicles.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/** Authentication and role rules of the vehicle endpoints. */
class ApiSecurityTest extends AbstractIntegrationTest {

  private static final String VEHICLES = "/api/vehicles";
  private static final String INTERNAL_KEY_HEADER = "X-Internal-Key";
  private static final String AVAILABLE_BODY = "{\"status\":\"AVAILABLE\"}";

  @Test
  void request_withoutCredentials_isUnauthorized() throws Exception {
    mockMvc
        .perform(get(VEHICLES))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  void request_withMalformedToken_isUnauthorized() throws Exception {
    mockMvc
        .perform(get(VEHICLES).header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
  }

  @Test
  void request_withExpiredToken_returnsTokenExpired() throws Exception {
    mockMvc
        .perform(
            get(VEHICLES).header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken("MANAGER")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("TOKEN_EXPIRED"));
  }

  @Test
  void request_withUnknownRole_isForbidden() throws Exception {
    mockMvc
        .perform(get(VEHICLES).header(HttpHeaders.AUTHORIZATION, bearer("DRIVER")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("FORBIDDEN"));
  }

  @Test
  void request_withManagerToken_isAllowed() throws Exception {
    mockMvc
        .perform(get(VEHICLES).header(HttpHeaders.AUTHORIZATION, bearer("MANAGER")))
        .andExpect(status().isOk());
  }

  @Test
  void request_withAdminToken_isAllowed() throws Exception {
    mockMvc
        .perform(get(VEHICLES).header(HttpHeaders.AUTHORIZATION, bearer("ADMIN")))
        .andExpect(status().isOk());
  }

  @Test
  void request_withInternalKey_isAllowed() throws Exception {
    mockMvc
        .perform(get(VEHICLES + "/summary").header(INTERNAL_KEY_HEADER, INTERNAL_KEY))
        .andExpect(status().isOk());
  }

  @Test
  void request_withWrongInternalKey_isUnauthorized() throws Exception {
    mockMvc
        .perform(get(VEHICLES).header(INTERNAL_KEY_HEADER, "wrong"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
  }

  @Test
  void internalKey_cannotReactivateOutOfServiceVehicle() throws Exception {
    Vehicle vehicle = saveVehicle("1111-AAA", VehicleType.VAN, VehicleStatus.OUT_OF_SERVICE);

    mockMvc
        .perform(
            patch(VEHICLES + "/{id}/status", vehicle.getId())
                .header(INTERNAL_KEY_HEADER, INTERNAL_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(AVAILABLE_BODY))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("FORBIDDEN"));
  }

  @Test
  void internalKey_canMoveVehicleToInUse() throws Exception {
    Vehicle vehicle = saveVehicle("2222-BBB", VehicleType.VAN, VehicleStatus.AVAILABLE);

    mockMvc
        .perform(
            patch(VEHICLES + "/{id}/status", vehicle.getId())
                .header(INTERNAL_KEY_HEADER, INTERNAL_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"IN_USE\",\"odometerKm\":45300}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_USE"))
        .andExpect(jsonPath("$.odometerKm").value(45_300));
  }

  @Test
  void adminToken_canReactivateOutOfServiceVehicle() throws Exception {
    Vehicle vehicle = saveVehicle("3333-CCC", VehicleType.VAN, VehicleStatus.OUT_OF_SERVICE);

    mockMvc
        .perform(
            patch(VEHICLES + "/{id}/status", vehicle.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(AVAILABLE_BODY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("AVAILABLE"));
  }

  @Test
  void managerToken_cannotReactivateOutOfServiceVehicle() throws Exception {
    Vehicle vehicle = saveVehicle("4444-DDD", VehicleType.VAN, VehicleStatus.OUT_OF_SERVICE);

    mockMvc
        .perform(
            patch(VEHICLES + "/{id}/status", vehicle.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer("MANAGER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(AVAILABLE_BODY))
        .andExpect(status().isForbidden());
  }

  @Test
  void health_isPublic() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }
}
