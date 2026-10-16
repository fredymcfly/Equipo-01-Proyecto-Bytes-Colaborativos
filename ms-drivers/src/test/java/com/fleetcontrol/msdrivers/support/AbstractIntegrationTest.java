package com.fleetcontrol.msdrivers.support;

import com.fleetcontrol.msdrivers.models.entities.Driver;
import com.fleetcontrol.msdrivers.models.enums.DriverStatus;
import com.fleetcontrol.msdrivers.models.enums.LicenseCategory;
import com.fleetcontrol.msdrivers.repository.DriverRepository;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Base class of the integration tests: full application on a real PostgreSQL (Testcontainers).
 * Every test class that extends it shares the same context, so the container starts only once.
 *
 * <p>Run without Docker with {@code mvn test -DexcludedGroups=integration}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestcontainersConfig.class)
@Tag("integration")
public abstract class AbstractIntegrationTest {

  protected static final String JWT_SECRET = "test-secret-for-integration-tests-0123456789abcdef";
  protected static final String INTERNAL_KEY = "test-internal-key";

  @Autowired protected MockMvc mockMvc;
  @Autowired protected DriverRepository driverRepository;

  @BeforeEach
  protected void cleanDatabase() {
    driverRepository.deleteAll();
  }

  /** Saves an active driver with the given license number and expiration date. */
  protected Driver saveDriver(String licenseNumber, LocalDate licenseExpiresAt) {
    return saveDriver(licenseNumber, licenseExpiresAt, DriverStatus.ACTIVE);
  }

  /** Saves a driver with the given license number, expiration date and status. */
  protected Driver saveDriver(
      String licenseNumber, LocalDate licenseExpiresAt, DriverStatus status) {
    return saveDriver("Driver " + licenseNumber, licenseNumber, licenseExpiresAt, status);
  }

  /** Saves a driver with the given name, license number, expiration date and status. */
  protected Driver saveDriver(
      String fullName, String licenseNumber, LocalDate licenseExpiresAt, DriverStatus status) {
    Driver driver = new Driver();
    driver.setFullName(fullName);
    driver.setEmail("driver@fleetcontrol.com");
    driver.setPhone("+34 600 123 456");
    driver.setLicenseNumber(licenseNumber);
    driver.setLicenseCategory(LicenseCategory.B);
    driver.setLicenseExpiresAt(licenseExpiresAt);
    driver.setStatus(status);
    return driverRepository.saveAndFlush(driver);
  }

  /** Signed token with the given role, valid for five minutes. */
  protected static String token(String role) {
    return sign(role, Instant.now().plusSeconds(300));
  }

  /** Signed token that expired an hour ago. */
  protected static String expiredToken(String role) {
    return sign(role, Instant.now().minusSeconds(3600));
  }

  /** Value of the Authorization header for a valid token of the given role. */
  protected static String bearer(String role) {
    return "Bearer " + token(role);
  }

  private static String sign(String role, Instant expiresAt) {
    try {
      JWTClaimsSet claims =
          new JWTClaimsSet.Builder()
              .subject(UUID.randomUUID().toString())
              .claim("username", "tester")
              .claim("role", role)
              .issueTime(Date.from(expiresAt.minusSeconds(7200)))
              .expirationTime(Date.from(expiresAt))
              .build();
      SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
      jwt.sign(new MACSigner(JWT_SECRET.getBytes(StandardCharsets.UTF_8)));
      return jwt.serialize();
    } catch (JOSEException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
