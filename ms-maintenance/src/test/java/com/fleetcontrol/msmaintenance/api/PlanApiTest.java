package com.fleetcontrol.msmaintenance.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fleetcontrol.msmaintenance.exception.ServiceUnavailableException;
import com.fleetcontrol.msmaintenance.exception.VehicleNotFoundException;
import com.fleetcontrol.msmaintenance.models.dto.internal.VehicleResponse;
import com.fleetcontrol.msmaintenance.models.enums.MaintenanceType;
import com.fleetcontrol.msmaintenance.support.AbstractIntegrationTest;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class PlanApiTest extends AbstractIntegrationTest {

  private static final String PLANS = "/api/maintenance/plans";

  private final UUID vehicleId = UUID.randomUUID();

  private String body(String type, String intervals) {
    return """
        {"vehicleId": "%s", "type": "%s"%s}
        """
        .formatted(vehicleId, type, intervals);
  }

  private ResultActions createPlan(String json) throws Exception {
    return mockMvc.perform(
        post(PLANS)
            .header(HttpHeaders.AUTHORIZATION, bearer("MANAGER"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(json));
  }

  @Test
  void create_validRequest_returnsCreatedPlanCalculatedFromVehicleOdometer() throws Exception {
    when(vehicleGateway.getVehicle(vehicleId))
        .thenReturn(new VehicleResponse(vehicleId, "AVAILABLE", 42_000));

    createPlan(body("OIL_CHANGE", ", \"intervalKm\": 10000, \"intervalDays\": 180"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.vehicleId").value(vehicleId.toString()))
        .andExpect(jsonPath("$.type").value("OIL_CHANGE"))
        .andExpect(jsonPath("$.lastDoneKm").value(42_000))
        .andExpect(jsonPath("$.nextDueKm").value(52_000))
        .andExpect(jsonPath("$.active").value(true));

    assertThat(planRepository.count()).isEqualTo(1);
  }

  @Test
  void create_secondActivePlanOfSameType_returnsConflict() throws Exception {
    when(vehicleGateway.getVehicle(vehicleId))
        .thenReturn(new VehicleResponse(vehicleId, "AVAILABLE", 1_000));
    String json = body("OIL_CHANGE", ", \"intervalKm\": 10000");

    createPlan(json).andExpect(status().isCreated());
    createPlan(json)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value("PLAN_ALREADY_EXISTS"));

    assertThat(planRepository.count()).isEqualTo(1);
  }

  @Test
  void create_withoutAnyInterval_returnsValidationError() throws Exception {
    createPlan(body("OIL_CHANGE", ""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details").isNotEmpty());
  }

  @Test
  void create_negativeInterval_returnsValidationError() throws Exception {
    createPlan(body("OIL_CHANGE", ", \"intervalKm\": -5"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
  }

  @Test
  void create_unknownVehicle_returnsNotFound() throws Exception {
    when(vehicleGateway.getVehicle(any())).thenThrow(new VehicleNotFoundException());

    createPlan(body("OIL_CHANGE", ", \"intervalKm\": 10000"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("VEHICLE_NOT_FOUND"));
  }

  @Test
  void create_vehicleServiceDown_returnsServiceUnavailable() throws Exception {
    when(vehicleGateway.getVehicle(any()))
        .thenThrow(new ServiceUnavailableException("ms-vehicles"));

    createPlan(body("OIL_CHANGE", ", \"intervalKm\": 10000"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.error").value("SERVICE_UNAVAILABLE"))
        .andExpect(jsonPath("$.service").value("ms-vehicles"));
  }

  @Test
  void list_filtersByVehicleAndPaginates() throws Exception {
    UUID otherVehicle = UUID.randomUUID();
    LocalDate lastDoneAt = LocalDate.now().minusDays(10);
    savePlan(vehicleId, MaintenanceType.OIL_CHANGE, lastDoneAt);
    savePlan(vehicleId, MaintenanceType.BRAKE_CHECK, lastDoneAt);
    savePlan(otherVehicle, MaintenanceType.OIL_CHANGE, lastDoneAt);

    mockMvc
        .perform(
            get(PLANS)
                .param("vehicle", vehicleId.toString())
                .header(HttpHeaders.AUTHORIZATION, bearer("MANAGER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.content.length()").value(2));

    mockMvc
        .perform(
            get(PLANS)
                .param("size", "1")
                .param("page", "0")
                .header(HttpHeaders.AUTHORIZATION, bearer("MANAGER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.size").value(1))
        .andExpect(jsonPath("$.totalElements").value(3))
        .andExpect(jsonPath("$.totalPages").value(3));
  }

  @Test
  void list_filtersByType() throws Exception {
    LocalDate lastDoneAt = LocalDate.now().minusDays(10);
    savePlan(vehicleId, MaintenanceType.OIL_CHANGE, lastDoneAt);
    savePlan(vehicleId, MaintenanceType.BRAKE_CHECK, lastDoneAt);

    mockMvc
        .perform(
            get(PLANS)
                .param("type", "BRAKE_CHECK")
                .header(HttpHeaders.AUTHORIZATION, bearer("MANAGER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].type").value("BRAKE_CHECK"));
  }

  @Test
  void list_invalidParameters_returnValidationError() throws Exception {
    mockMvc
        .perform(
            get(PLANS).param("size", "500").header(HttpHeaders.AUTHORIZATION, bearer("MANAGER")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

    mockMvc
        .perform(
            get(PLANS).param("type", "FLYING").header(HttpHeaders.AUTHORIZATION, bearer("MANAGER")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
  }
}
