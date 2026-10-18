package com.fleetcontrol.msfuel.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fleetcontrol.msfuel.config.SecurityConfig;
import com.fleetcontrol.msfuel.config.SecurityErrorHandler;
import com.fleetcontrol.msfuel.dto.FuelConsumptionResponse;
import com.fleetcontrol.msfuel.dto.FuelRecordResponse;
import com.fleetcontrol.msfuel.dto.FuelStatsResponse;
import com.fleetcontrol.msfuel.dto.Granularity;
import com.fleetcontrol.msfuel.dto.PageResponse;
import com.fleetcontrol.msfuel.dto.StatsTotals;
import com.fleetcontrol.msfuel.exception.DependencyUnavailableException;
import com.fleetcontrol.msfuel.exception.ErrorCode;
import com.fleetcontrol.msfuel.model.FuelType;
import com.fleetcontrol.msfuel.service.FuelService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FuelController.class)
@Import({SecurityConfig.class, SecurityErrorHandler.class})
@TestPropertySource(
    properties = {
      "internal.api-key=test-internal-key",
      "fleet.jwt.secret=test-secret-for-integration-tests-0123456789"
    })
@WithMockUser(roles = "MANAGER")
class FuelControllerTest {

  private static final UUID VEHICLE = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final String FROM = "2026-09-01";
  private static final String TO = "2026-09-30";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private FuelService fuelService;

  @Test
  void registersRefuel() throws Exception {
    given(fuelService.saveReFuel(any())).willReturn(response());

    mockMvc
        .perform(
            post("/api/fuel/refuels")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "vehicleId": "11111111-1111-1111-1111-111111111111",
                      "refueledAt": "2026-09-01T10:00:00Z",
                      "liters": 40.5,
                      "pricePerLiter": 1.50,
                      "odometerKm": 1000,
                      "fullTank": true,
                      "station": "Repsol"
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(VEHICLE.toString()))
        .andExpect(jsonPath("$.totalCost").value(60.75));
  }

  @Test
  void rejectsBodyWithInvalidLiters() throws Exception {
    mockMvc
        .perform(
            post("/api/fuel/refuels")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "vehicleId": "11111111-1111-1111-1111-111111111111",
                      "refueledAt": "2026-09-01T10:00:00Z",
                      "liters": -1,
                      "pricePerLiter": 1.50,
                      "odometerKm": 1000,
                      "fullTank": true
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details[0].field").value("liters"));
  }

  @Test
  void rejectsPageSizeAboveTheLimit() throws Exception {
    mockMvc
        .perform(get("/api/fuel/refuels").param("size", "500"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
  }

  @Test
  void listsRefuels() throws Exception {
    given(fuelService.list(any(), any(), any(), eq(0), eq(20)))
        .willReturn(new PageResponse<>(List.of(response()), 0, 20, 1, 1));

    mockMvc
        .perform(get("/api/fuel/refuels"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(VEHICLE.toString()))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void returnsConsumption() throws Exception {
    given(fuelService.consumption(any(), any()))
        .willReturn(
            new FuelConsumptionResponse(LocalDate.parse(FROM), LocalDate.parse(TO), List.of()));

    mockMvc
        .perform(get("/api/fuel/consumption").param("from", FROM).param("to", TO))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.from").value(FROM))
        .andExpect(jsonPath("$.vehicles").isEmpty());
  }

  @Test
  void returnsStatsWithDefaultGranularity() throws Exception {
    given(fuelService.stats(any(), any(), any(), any()))
        .willReturn(
            new FuelStatsResponse(
                LocalDate.parse(FROM),
                LocalDate.parse(TO),
                Granularity.DAY,
                new StatsTotals(BigDecimal.ZERO, BigDecimal.ZERO, null, null),
                List.of()));

    mockMvc
        .perform(get("/api/fuel/stats").param("from", FROM).param("to", TO))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.granularity").value("DAY"))
        .andExpect(jsonPath("$.series").isEmpty());
  }

  @Test
  void rejectsUnknownGranularity() throws Exception {
    mockMvc
        .perform(
            get("/api/fuel/stats").param("from", FROM).param("to", TO).param("granularity", "YEAR"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
  }

  @Test
  void reportsDependencyUnavailableWhenVehiclesIsDown() throws Exception {
    given(fuelService.saveReFuel(any()))
        .willThrow(
            new DependencyUnavailableException("ms-vehicles", ErrorCode.SERVICE_UNAVAILABLE));

    mockMvc
        .perform(
            post("/api/fuel/refuels")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "vehicleId": "11111111-1111-1111-1111-111111111111",
                      "refueledAt": "2026-09-01T10:00:00Z",
                      "liters": 40.5,
                      "pricePerLiter": 1.50,
                      "odometerKm": 1000,
                      "fullTank": true
                    }
                    """))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.error").value("SERVICE_UNAVAILABLE"))
        .andExpect(jsonPath("$.service").value("ms-vehicles"));
  }

  private static FuelRecordResponse response() {
    return new FuelRecordResponse(
        VEHICLE,
        VEHICLE,
        Instant.parse("2026-09-01T10:00:00Z"),
        FuelType.DIESEL,
        new BigDecimal("40.50"),
        new BigDecimal("1.50"),
        new BigDecimal("60.75"),
        1000,
        true,
        "Repsol",
        null);
  }
}
