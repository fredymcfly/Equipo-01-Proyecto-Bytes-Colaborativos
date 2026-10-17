package com.fleetcontrol.msalerts.support;

import com.fleetcontrol.msalerts.client.DriversClient;
import com.fleetcontrol.msalerts.client.FuelClient;
import com.fleetcontrol.msalerts.client.MaintenanceClient;
import com.fleetcontrol.msalerts.client.VehiclesClient;
import com.fleetcontrol.msalerts.models.entities.Alert;
import com.fleetcontrol.msalerts.models.entities.AlertRule;
import com.fleetcontrol.msalerts.models.enums.AlertSeverity;
import com.fleetcontrol.msalerts.models.enums.AlertStatus;
import com.fleetcontrol.msalerts.models.enums.AlertType;
import com.fleetcontrol.msalerts.repository.AlertRepository;
import com.fleetcontrol.msalerts.repository.AlertRuleRepository;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Base class of the integration tests: full application on a real PostgreSQL (Testcontainers),
 * with the clients of the source services replaced by mocks. Every test class that extends it
 * shares the same context, so the container starts only once.
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
  protected static final String INTERNAL_KEY_HEADER = "X-Internal-Key";

  @Autowired protected MockMvc mockMvc;
  @Autowired protected AlertRepository alertRepository;
  @Autowired protected AlertRuleRepository ruleRepository;

  @MockitoBean protected MaintenanceClient maintenanceClient;
  @MockitoBean protected DriversClient driversClient;
  @MockitoBean protected FuelClient fuelClient;
  @MockitoBean protected VehiclesClient vehiclesClient;

  @BeforeEach
  protected void resetState() {
    alertRepository.deleteAll();
    resetRule(AlertType.MAINTENANCE_DUE, AlertSeverity.WARNING, "daysBefore", 7);
    resetRule(AlertType.MAINTENANCE_OVERDUE, AlertSeverity.CRITICAL, null, 0);
    resetRule(AlertType.LICENSE_EXPIRING, AlertSeverity.WARNING, "daysBefore", 30);
    resetRule(AlertType.LICENSE_EXPIRED, AlertSeverity.CRITICAL, null, 0);
    resetRule(AlertType.HIGH_CONSUMPTION, AlertSeverity.WARNING, "thresholdPercent", 25);
  }

  /** Saves an alert with a dedup key made from its type and a random suffix. */
  protected Alert saveAlert(
      AlertType type, AlertSeverity severity, AlertStatus status, Instant createdAt) {
    return saveAlert(type, severity, status, createdAt, null);
  }

  /** Saves an alert about the given vehicle. */
  protected Alert saveAlert(
      AlertType type,
      AlertSeverity severity,
      AlertStatus status,
      Instant createdAt,
      UUID vehicleId) {
    Alert alert = new Alert();
    alert.setType(type);
    alert.setSeverity(severity);
    alert.setStatus(status);
    alert.setVehicleId(vehicleId);
    alert.setMessage("Alert " + type);
    alert.setDedupKey(type + ":" + UUID.randomUUID());
    alert.setCreatedAt(createdAt);
    return alertRepository.saveAndFlush(alert);
  }

  /** Signed token with the given role, valid for five minutes. */
  protected static String token(String role) {
    return sign(role, UUID.randomUUID(), Instant.now().plusSeconds(300));
  }

  /** Signed token that expired an hour ago. */
  protected static String expiredToken(String role) {
    return sign(role, UUID.randomUUID(), Instant.now().minusSeconds(3600));
  }

  /** Value of the Authorization header for a valid token of the given role. */
  protected static String bearer(String role) {
    return "Bearer " + token(role);
  }

  /** Value of the Authorization header for a valid token of the given user. */
  protected static String bearer(String role, UUID userId) {
    return "Bearer " + sign(role, userId, Instant.now().plusSeconds(300));
  }

  private void resetRule(AlertType type, AlertSeverity severity, String param, int value) {
    AlertRule rule = ruleRepository.findById(type).orElseThrow();
    rule.setEnabled(true);
    rule.setSeverity(severity);
    Map<String, Object> params = new HashMap<>();
    if (param != null) {
      params.put(param, value);
    }
    rule.setParams(params);
    ruleRepository.saveAndFlush(rule);
  }

  private static String sign(String role, UUID userId, Instant expiresAt) {
    try {
      JWTClaimsSet claims =
          new JWTClaimsSet.Builder()
              .subject(userId.toString())
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
