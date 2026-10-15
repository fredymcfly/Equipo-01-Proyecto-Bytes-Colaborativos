package com.fleetcontrol.msmaintenance.support;

import com.fleetcontrol.msmaintenance.client.VehicleGateway;
import com.fleetcontrol.msmaintenance.models.entities.MaintenanceOrder;
import com.fleetcontrol.msmaintenance.models.entities.MaintenancePlan;
import com.fleetcontrol.msmaintenance.models.enums.MaintenanceOrderStatus;
import com.fleetcontrol.msmaintenance.models.enums.MaintenanceType;
import com.fleetcontrol.msmaintenance.repository.MaintenanceOrderRepository;
import com.fleetcontrol.msmaintenance.repository.MaintenancePlanRepository;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Base class of the integration tests: full application on a real PostgreSQL (Testcontainers), with
 * ms-vehicles replaced by a mock. Every test class that extends it shares the same context, so the
 * container starts only once.
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
  @Autowired protected MaintenancePlanRepository planRepository;
  @Autowired protected MaintenanceOrderRepository orderRepository;

  @MockitoBean protected VehicleGateway vehicleGateway;

  @BeforeEach
  protected void cleanDatabase() {
    orderRepository.deleteAll();
    planRepository.deleteAll();
  }

  /** Saves an active plan of 10,000 km / 90 days whose last intervention was at 10,000 km. */
  protected MaintenancePlan savePlan(UUID vehicleId, MaintenanceType type, LocalDate lastDoneAt) {
    MaintenancePlan plan = new MaintenancePlan();
    plan.setVehicleId(vehicleId);
    plan.setType(type);
    plan.setIntervalKm(10_000);
    plan.setIntervalDays(90);
    plan.registerDone(lastDoneAt, 10_000);
    return planRepository.saveAndFlush(plan);
  }

  /** Saves an order of the given plan with the given status. */
  protected MaintenanceOrder saveOrder(
      MaintenancePlan plan, MaintenanceOrderStatus status, LocalDate scheduledFor) {
    MaintenanceOrder order = new MaintenanceOrder();
    order.setPlanId(plan.getId());
    order.setVehicleId(plan.getVehicleId());
    order.setType(plan.getType());
    order.setStatus(status);
    order.setScheduledFor(scheduledFor);
    return orderRepository.saveAndFlush(order);
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
