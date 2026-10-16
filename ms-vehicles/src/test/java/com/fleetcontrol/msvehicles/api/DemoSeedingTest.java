package com.fleetcontrol.msvehicles.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fleetcontrol.msvehicles.config.properties.DemoProperties;
import com.fleetcontrol.msvehicles.seeder.DemoDataSeeder;
import com.fleetcontrol.msvehicles.support.AbstractIntegrationTest;
import java.time.Clock;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

/** The generated demo data must be persisted and readable through the API. */
class DemoSeedingTest extends AbstractIntegrationTest {

  private DemoDataSeeder seeder;

  @BeforeEach
  void createSeeder() {
    seeder = new DemoDataSeeder(vehicleRepository, new DemoProperties(42, 90), Clock.systemUTC());
  }

  @Test
  void seeder_persistsTheDemoFleetAndTheVehiclesAreReadableThroughTheApi() throws Exception {
    seeder.run(null);

    assertThat(vehicleRepository.count()).isEqualTo(25);
    UUID id = vehicleRepository.findAll().get(0).getId();
    assertThat(id).isNotNull();
    mockMvc
        .perform(get("/api/vehicles/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer("MANAGER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id.toString()));
  }

  @Test
  void seeder_isIdempotent() {
    seeder.run(null);
    seeder.run(null);

    assertThat(vehicleRepository.count()).isEqualTo(25);
  }

  @Test
  void summary_ofTheDemoFleetMatchesTheDocumentedCounts() throws Exception {
    seeder.run(null);

    mockMvc
        .perform(get("/api/vehicles/summary").header(HttpHeaders.AUTHORIZATION, bearer("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(25))
        .andExpect(jsonPath("$.byStatus.AVAILABLE").value(14))
        .andExpect(jsonPath("$.byStatus.IN_USE").value(8))
        .andExpect(jsonPath("$.byStatus.IN_MAINTENANCE").value(2))
        .andExpect(jsonPath("$.byStatus.OUT_OF_SERVICE").value(1))
        .andExpect(jsonPath("$.byType.CAR").value(9))
        .andExpect(jsonPath("$.byType.VAN").value(10))
        .andExpect(jsonPath("$.byType.TRUCK").value(4))
        .andExpect(jsonPath("$.byType.MOTORCYCLE").value(2));
  }
}
