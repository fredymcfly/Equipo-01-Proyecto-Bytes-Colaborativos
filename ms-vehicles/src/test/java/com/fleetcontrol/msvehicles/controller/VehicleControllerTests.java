package com.fleetcontrol.msvehicles.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetcontrol.msvehicles.dto.CreateVehicleRequest;
import com.fleetcontrol.msvehicles.dto.UpdateVehicleRequest;
import com.fleetcontrol.msvehicles.dto.VehicleStatusRequest;
import com.fleetcontrol.msvehicles.model.FuelType;
import com.fleetcontrol.msvehicles.model.VehicleStatus;
import com.fleetcontrol.msvehicles.model.VehicleType;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for the vehicle endpoints. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "MANAGER")
class VehicleControllerTests {

  private static final String ENDPOINT = "/api/vehicles";
  private static final String PLATE = "IT-4821-KDF";
  private static final String MAKE = "Ford";
  private static final String MODEL = "Transit";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void createsVehicleAsAvailable() throws Exception {
    mockMvc
        .perform(
            post(ENDPOINT)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(createRequest())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.plate").value(PLATE))
        .andExpect(jsonPath("$.status").value("AVAILABLE"));
  }

  @Test
  void rejectsDuplicatedPlate() throws Exception {
    createVehicle();

    mockMvc
        .perform(
            post(ENDPOINT)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(createRequest())))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value("VEHICLE_ALREADY_EXISTS"));
  }

  @Test
  void rejectsInvalidBodyWithDetails() throws Exception {
    CreateVehicleRequest invalid =
        new CreateVehicleRequest(
            PLATE, MAKE, MODEL, 1980, VehicleType.VAN, FuelType.DIESEL, 0, 45210);

    mockMvc
        .perform(
            post(ENDPOINT)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(invalid)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details").isArray())
        .andExpect(jsonPath("$.details").isNotEmpty());
  }

  @Test
  void listsVehiclesWithPaginationEnvelope() throws Exception {
    mockMvc
        .perform(get(ENDPOINT).param("size", "5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(5))
        .andExpect(jsonPath("$.totalElements").exists())
        .andExpect(jsonPath("$.totalPages").exists());
  }

  @Test
  void rejectsPageSizeAboveMaximum() throws Exception {
    mockMvc
        .perform(get(ENDPOINT).param("size", "101"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
  }

  @Test
  void returnsNotFoundForUnknownVehicle() throws Exception {
    mockMvc
        .perform(get(ENDPOINT + "/{vehicleId}", UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("VEHICLE_NOT_FOUND"));
  }

  @Test
  void updatesDescriptiveDataKeepingPlateAndStatus() throws Exception {
    String created = createVehicle();
    String vehicleId = objectMapper.readTree(created).get("id").asText();
    UpdateVehicleRequest update =
        new UpdateVehicleRequest(
            MAKE, "Transit Custom", 2023, VehicleType.VAN, FuelType.DIESEL, 85);

    mockMvc
        .perform(
            put(ENDPOINT + "/{vehicleId}", vehicleId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(update)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.model").value("Transit Custom"))
        .andExpect(jsonPath("$.plate").value(PLATE))
        .andExpect(jsonPath("$.status").value("AVAILABLE"));
  }

  @Test
  void changesVehicleStatus() throws Exception {
    String vehicleId = createVehicleId();

    mockMvc
        .perform(
            patch(ENDPOINT + "/{vehicleId}/status", vehicleId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(new VehicleStatusRequest(VehicleStatus.IN_USE, null))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_USE"));
  }

  @Test
  void rejectsSameStatusTransition() throws Exception {
    String vehicleId = createVehicleId();

    mockMvc
        .perform(
            patch(ENDPOINT + "/{vehicleId}/status", vehicleId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(new VehicleStatusRequest(VehicleStatus.AVAILABLE, null))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value("INVALID_STATUS_TRANSITION"));
  }

  @Test
  void rejectsLowerOdometer() throws Exception {
    String vehicleId = createVehicleId();

    mockMvc
        .perform(
            patch(ENDPOINT + "/{vehicleId}/status", vehicleId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(new VehicleStatusRequest(VehicleStatus.IN_USE, 1))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value("INVALID_ODOMETER"));
  }

  @Test
  void forbidsManagerReactivatingOutOfService() throws Exception {
    String vehicleId = createVehicleId();
    changeStatus(vehicleId, new VehicleStatusRequest(VehicleStatus.OUT_OF_SERVICE, null));

    mockMvc
        .perform(
            patch(ENDPOINT + "/{vehicleId}/status", vehicleId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(new VehicleStatusRequest(VehicleStatus.AVAILABLE, null))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("FORBIDDEN"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void allowsAdminReactivatingOutOfService() throws Exception {
    String vehicleId = createVehicleId();
    changeStatus(vehicleId, new VehicleStatusRequest(VehicleStatus.OUT_OF_SERVICE, null));

    mockMvc
        .perform(
            patch(ENDPOINT + "/{vehicleId}/status", vehicleId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(new VehicleStatusRequest(VehicleStatus.AVAILABLE, null))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("AVAILABLE"));
  }

  @Test
  void returnsNotFoundWhenChangingUnknownVehicle() throws Exception {
    mockMvc
        .perform(
            patch(ENDPOINT + "/{vehicleId}/status", UUID.randomUUID())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(new VehicleStatusRequest(VehicleStatus.IN_USE, null))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("VEHICLE_NOT_FOUND"));
  }

  @Test
  void rejectsStatusBodyWithoutStatus() throws Exception {
    String vehicleId = createVehicleId();

    mockMvc
        .perform(
            patch(ENDPOINT + "/{vehicleId}/status", vehicleId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
  }

  private String createVehicleId() throws Exception {
    return objectMapper.readTree(createVehicle()).get("id").asText();
  }

  private void changeStatus(String vehicleId, VehicleStatusRequest request) throws Exception {
    mockMvc
        .perform(
            patch(ENDPOINT + "/{vehicleId}/status", vehicleId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(request)))
        .andExpect(status().isOk());
  }

  private String createVehicle() throws Exception {
    return mockMvc
        .perform(
            post(ENDPOINT)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(createRequest())))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  private String json(Object payload) throws Exception {
    return objectMapper.writeValueAsString(payload);
  }

  private static CreateVehicleRequest createRequest() {
    return new CreateVehicleRequest(
        PLATE, MAKE, MODEL, 2022, VehicleType.VAN, FuelType.DIESEL, 80, 45210);
  }
}
