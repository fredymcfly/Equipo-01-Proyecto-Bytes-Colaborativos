package com.fleetcontrol.msdrivers.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fleetcontrol.msdrivers.models.entities.Driver;
import com.fleetcontrol.msdrivers.models.enums.DriverStatus;
import com.fleetcontrol.msdrivers.support.AbstractIntegrationTest;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** Integration tests for the driver endpoints. */
class DriverControllerTests extends AbstractIntegrationTest {

  private static final String ENDPOINT = "/api/drivers";
  private static final String LICENSE = "B-5120377";
  private static final String OTHER_LICENSE = "C-2038841";

  // ---------------------------------------------------------------- create

  @Test
  void create_registersTheDriverAsActive() throws Exception {
    postDriver(createJson(LICENSE, LocalDate.now().plusYears(3)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.fullName").value("Sara Ibáñez"))
        .andExpect(jsonPath("$.licenseNumber").value(LICENSE))
        .andExpect(jsonPath("$.licenseCategory").value("B"))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.createdAt").exists())
        .andExpect(jsonPath("$.updatedAt").exists());
  }

  @Test
  void create_ignoresTheStatusSentByTheClient() throws Exception {
    String body =
        """
        {
          "fullName": "Sara Ibáñez",
          "email": "sara.ibanez@fleetcontrol.com",
          "licenseNumber": "%s",
          "licenseCategory": "B",
          "licenseExpiresAt": "%s",
          "status": "SUSPENDED"
        }
        """
            .formatted(LICENSE, LocalDate.now().plusYears(3));

    postDriver(body)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("ACTIVE"));
  }

  @Test
  void create_withExistingLicense_returnsConflict() throws Exception {
    saveDriver(LICENSE, LocalDate.now().plusYears(2));

    postDriver(createJson(LICENSE, LocalDate.now().plusYears(3)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value("DRIVER_ALREADY_EXISTS"))
        .andExpect(jsonPath("$.message").exists())
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  void create_withExpirationInThePast_isRejected() throws Exception {
    postDriver(createJson(LICENSE, LocalDate.now().minusDays(1)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details[0].field").value("licenseExpiresAt"));
  }

  @Test
  void create_withInvalidEmail_isRejected() throws Exception {
    String body = createJson(LICENSE, LocalDate.now().plusYears(3)).replace("sara.ibanez@", "sara");

    postDriver(body)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details[0].field").value("email"));
  }

  @Test
  void create_withoutRequiredFields_listsEveryInvalidField() throws Exception {
    postDriver("{}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details.length()").value(5));
  }

  @Test
  void create_withUnknownCategory_isRejected() throws Exception {
    String body = createJson(LICENSE, LocalDate.now().plusYears(3)).replace("\"B\"", "\"Z\"");

    postDriver(body)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
  }

  // ---------------------------------------------------------------- get

  @Test
  void get_returnsTheDetailOfTheDriver() throws Exception {
    Driver driver = saveDriver(LICENSE, LocalDate.now().plusYears(2));

    mockMvc
        .perform(
            get(ENDPOINT + "/{id}", driver.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer("MANAGER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(driver.getId().toString()))
        .andExpect(jsonPath("$.licenseNumber").value(LICENSE))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.createdAt").exists())
        .andExpect(jsonPath("$.updatedAt").exists());
  }

  @Test
  void get_ofAnUnknownDriver_returnsNotFound() throws Exception {
    mockMvc
        .perform(
            get(ENDPOINT + "/{id}", UUID.randomUUID())
                .header(HttpHeaders.AUTHORIZATION, bearer("MANAGER")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("DRIVER_NOT_FOUND"))
        .andExpect(jsonPath("$.message").exists())
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  void get_withMalformedId_isRejected() throws Exception {
    mockMvc
        .perform(get(ENDPOINT + "/not-a-uuid").header(HttpHeaders.AUTHORIZATION, bearer("MANAGER")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details[0].field").value("driverId"));
  }

  // ---------------------------------------------------------------- list

  @Test
  void list_returnsTheFirstPageWithTheSummaryOfEveryDriver() throws Exception {
    saveDriver("B-1000001", LocalDate.now().plusYears(2));
    saveDriver("B-1000002", LocalDate.now().plusYears(2));
    saveDriver("B-1000003", LocalDate.now().plusYears(2));

    list("")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(3))
        .andExpect(jsonPath("$.content[0].licenseNumber").exists())
        .andExpect(jsonPath("$.content[0].createdAt").doesNotExist())
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(20))
        .andExpect(jsonPath("$.totalElements").value(3))
        .andExpect(jsonPath("$.totalPages").value(1));
  }

  @Test
  void list_paginatesTheResult() throws Exception {
    saveDriver("B-1000001", LocalDate.now().plusYears(2));
    saveDriver("B-1000002", LocalDate.now().plusYears(2));
    saveDriver("B-1000003", LocalDate.now().plusYears(2));

    list("?page=1&size=2")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.page").value(1))
        .andExpect(jsonPath("$.size").value(2))
        .andExpect(jsonPath("$.totalElements").value(3))
        .andExpect(jsonPath("$.totalPages").value(2));
  }

  @Test
  void list_ordersTheDriversByName() throws Exception {
    saveDriver("Zoe Vidal", "B-1000001", LocalDate.now().plusYears(2), DriverStatus.ACTIVE);
    saveDriver("Ana Ruiz", "B-1000002", LocalDate.now().plusYears(2), DriverStatus.ACTIVE);

    list("")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].fullName").value("Ana Ruiz"))
        .andExpect(jsonPath("$.content[1].fullName").value("Zoe Vidal"));
  }

  @Test
  void list_filtersByStatus() throws Exception {
    saveDriver("B-1000001", LocalDate.now().plusYears(2), DriverStatus.ACTIVE);
    saveDriver("B-1000002", LocalDate.now().plusYears(2), DriverStatus.ON_LEAVE);
    saveDriver("B-1000003", LocalDate.now().plusYears(2), DriverStatus.SUSPENDED);

    list("?status=ON_LEAVE")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].licenseNumber").value("B-1000002"));
  }

  @Test
  void list_filtersByLicenseExpirationIncludingTheExpiredOnes() throws Exception {
    saveDriver("B-1000001", LocalDate.now().plusDays(5));
    saveDriver("B-1000002", LocalDate.now().plusDays(40));
    saveDriver("B-1000003", LocalDate.now().minusDays(3));

    list("?licenseExpiringInDays=10")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(2));
    list("?licenseExpiringInDays=0")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].licenseNumber").value("B-1000003"));
    list("?licenseExpiringInDays=60")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(3));
  }

  @Test
  void list_combinesStatusAndLicenseExpiration() throws Exception {
    saveDriver("B-1000001", LocalDate.now().plusDays(5), DriverStatus.ACTIVE);
    saveDriver("B-1000002", LocalDate.now().plusDays(5), DriverStatus.SUSPENDED);

    list("?status=ACTIVE&licenseExpiringInDays=10")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].licenseNumber").value("B-1000001"));
  }

  @Test
  void list_withUnknownStatus_isRejected() throws Exception {
    list("?status=RETIRED")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details[0].field").value("status"));
  }

  @Test
  void list_withInvalidPagination_isRejected() throws Exception {
    list("?size=101").andExpect(status().isBadRequest());
    list("?size=0").andExpect(status().isBadRequest());
    list("?page=-1").andExpect(status().isBadRequest());
    list("?licenseExpiringInDays=-1")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
  }

  // ---------------------------------------------------------------- update

  @Test
  void update_changesEveryDataAndRenewsTheLicense() throws Exception {
    Driver driver = saveDriver(LICENSE, LocalDate.now().plusDays(5));
    LocalDate renewedUntil = LocalDate.now().plusYears(4);

    putDriver(
            driver.getId(),
            updateJson("Sara Ibáñez Díaz", LICENSE, renewedUntil.toString(), "ON_LEAVE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(driver.getId().toString()))
        .andExpect(jsonPath("$.fullName").value("Sara Ibáñez Díaz"))
        .andExpect(jsonPath("$.licenseExpiresAt").value(renewedUntil.toString()))
        .andExpect(jsonPath("$.status").value("ON_LEAVE"))
        .andExpect(jsonPath("$.updatedAt").exists());

    Driver saved = driverRepository.findById(driver.getId()).orElseThrow();
    assertThat(saved.getFullName()).isEqualTo("Sara Ibáñez Díaz");
    assertThat(saved.getStatus()).isEqualTo(DriverStatus.ON_LEAVE);
    assertThat(saved.getLicenseExpiresAt()).isEqualTo(renewedUntil);
  }

  @Test
  void update_canKeepTheOwnLicenseNumberAndLeaveAnExpiredLicense() throws Exception {
    Driver driver = saveDriver(LICENSE, LocalDate.now().minusDays(10));

    putDriver(
            driver.getId(),
            updateJson(
                "Sara Ibáñez", LICENSE, LocalDate.now().minusDays(10).toString(), "SUSPENDED"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUSPENDED"));
  }

  @Test
  void update_withTheLicenseOfAnotherDriver_returnsConflict() throws Exception {
    Driver driver = saveDriver(LICENSE, LocalDate.now().plusYears(2));
    saveDriver(OTHER_LICENSE, LocalDate.now().plusYears(2));

    putDriver(
            driver.getId(),
            updateJson(
                "Sara Ibáñez", OTHER_LICENSE, LocalDate.now().plusYears(2).toString(), "ACTIVE"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value("DRIVER_ALREADY_EXISTS"));
  }

  @Test
  void update_ofAnUnknownDriver_returnsNotFound() throws Exception {
    putDriver(
            UUID.randomUUID(),
            updateJson("Sara Ibáñez", LICENSE, LocalDate.now().plusYears(2).toString(), "ACTIVE"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("DRIVER_NOT_FOUND"));
  }

  @Test
  void update_withoutStatus_isRejected() throws Exception {
    Driver driver = saveDriver(LICENSE, LocalDate.now().plusYears(2));
    String body =
        updateJson("Sara Ibáñez", LICENSE, LocalDate.now().plusYears(2).toString(), "ACTIVE")
            .replace("\"status\": \"ACTIVE\"", "\"status\": null");

    putDriver(driver.getId(), body)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details[0].field").value("status"));
  }

  @Test
  void update_withUnknownStatus_isRejected() throws Exception {
    Driver driver = saveDriver(LICENSE, LocalDate.now().plusYears(2));

    putDriver(
            driver.getId(),
            updateJson("Sara Ibáñez", LICENSE, LocalDate.now().plusYears(2).toString(), "RETIRED"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
  }

  // ---------------------------------------------------------------- helpers

  private ResultActions postDriver(String body) throws Exception {
    return mockMvc.perform(
        post(ENDPOINT)
            .header(HttpHeaders.AUTHORIZATION, bearer("MANAGER"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  private ResultActions putDriver(UUID id, String body) throws Exception {
    return mockMvc.perform(
        put(ENDPOINT + "/{id}", id)
            .header(HttpHeaders.AUTHORIZATION, bearer("MANAGER"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  private ResultActions list(String query) throws Exception {
    return mockMvc.perform(
        get(ENDPOINT + query).header(HttpHeaders.AUTHORIZATION, bearer("MANAGER")));
  }

  private static String createJson(String license, LocalDate expiresAt) {
    return """
        {
          "fullName": "Sara Ibáñez",
          "email": "sara.ibanez@fleetcontrol.com",
          "phone": "+34 622 456 789",
          "licenseNumber": "%s",
          "licenseCategory": "B",
          "licenseExpiresAt": "%s"
        }
        """
        .formatted(license, expiresAt);
  }

  private static String updateJson(
      String fullName, String license, String expiresAt, String status) {
    return """
        {
          "fullName": "%s",
          "email": "sara.ibanez@fleetcontrol.com",
          "phone": "+34 622 456 789",
          "licenseNumber": "%s",
          "licenseCategory": "B",
          "licenseExpiresAt": "%s",
          "status": "%s"
        }
        """
        .formatted(fullName, license, expiresAt, status);
  }
}
