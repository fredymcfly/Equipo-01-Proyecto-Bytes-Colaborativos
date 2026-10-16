package com.fleetcontrol.msdrivers.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fleetcontrol.msdrivers.config.properties.DemoProperties;
import com.fleetcontrol.msdrivers.seeder.DemoDataSeeder;
import com.fleetcontrol.msdrivers.support.AbstractIntegrationTest;
import java.time.Clock;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

/** The generated demo data must be persisted and readable through the API. */
class DemoSeedingTest extends AbstractIntegrationTest {

  private static final String DRIVERS = "/api/drivers";

  private DemoDataSeeder seeder;

  @BeforeEach
  void createSeeder() {
    seeder = new DemoDataSeeder(driverRepository, new DemoProperties(42, 90), Clock.systemUTC());
  }

  @Test
  void seeder_persistsTheDemoDriversAndTheyAreReadableThroughTheApi() throws Exception {
    seeder.run(null);

    assertThat(driverRepository.count()).isEqualTo(20);
    UUID id = driverRepository.findAll().get(0).getId();
    assertThat(id).isNotNull();
    mockMvc
        .perform(get(DRIVERS + "/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer("MANAGER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id.toString()));
  }

  @Test
  void seeder_isIdempotent() {
    seeder.run(null);
    seeder.run(null);

    assertThat(driverRepository.count()).isEqualTo(20);
  }

  @Test
  void licenseFilter_findsTheThreeLicensesAboutToExpireAndTheExpiredOne() throws Exception {
    seeder.run(null);

    mockMvc
        .perform(
            get(DRIVERS)
                .param("licenseExpiringInDays", "30")
                .header(HttpHeaders.AUTHORIZATION, bearer("MANAGER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(4));
  }

  @Test
  void statusFilter_matchesTheDocumentedDistribution() throws Exception {
    seeder.run(null);

    mockMvc
        .perform(
            get(DRIVERS)
                .param("status", "ACTIVE")
                .header(HttpHeaders.AUTHORIZATION, bearer("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(16));
  }
}
