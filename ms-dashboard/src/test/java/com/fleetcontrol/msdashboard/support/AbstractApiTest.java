package com.fleetcontrol.msdashboard.support;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.fleetcontrol.msdashboard.client.AlertsClient;
import com.fleetcontrol.msdashboard.client.FuelClient;
import com.fleetcontrol.msdashboard.client.MaintenanceClient;
import com.fleetcontrol.msdashboard.client.RoutesClient;
import com.fleetcontrol.msdashboard.client.VehiclesClient;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Base class of the API tests: full application with the five source services replaced by mocks.
 * There is no database, so these tests need no Docker.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractApiTest {

  protected static final String JWT_SECRET = "test-secret-for-integration-tests-0123456789abcdef";
  protected static final String INTERNAL_KEY = "test-internal-key";

  @Autowired protected MockMvc mockMvc;
  @Autowired private CacheManager cacheManager;

  @MockitoBean protected VehiclesClient vehiclesClient;
  @MockitoBean protected RoutesClient routesClient;
  @MockitoBean protected FuelClient fuelClient;
  @MockitoBean protected MaintenanceClient maintenanceClient;
  @MockitoBean protected AlertsClient alertsClient;

  /** Empties the caches and makes every source service answer with its example data. */
  @BeforeEach
  protected void prepare() {
    cacheManager
        .getCacheNames()
        .forEach(name -> Objects.requireNonNull(cacheManager.getCache(name)).clear());
    stubEverySource();
  }

  /** Makes every source service answer with its example data. */
  protected void stubEverySource() {
    when(vehiclesClient.summary()).thenReturn(SourceData.fleet());
    when(vehiclesClient.getVehicle(SourceData.VAN_ID))
        .thenReturn(SourceData.vehicle(SourceData.VAN_ID, SourceData.VAN_PLATE));
    when(routesClient.stats(anyString(), anyString(), anyString()))
        .thenReturn(SourceData.routesStats());
    when(fuelClient.stats(anyString(), anyString(), anyString()))
        .thenReturn(SourceData.fuelStats());
    when(fuelClient.consumption(anyString(), anyString())).thenReturn(SourceData.consumption());
    when(maintenanceClient.stats(anyString(), anyString(), anyString()))
        .thenReturn(SourceData.maintenanceStats());
    when(maintenanceClient.orders(anyString(), anyInt(), anyInt())).thenReturn(SourceData.orders());
    when(alertsClient.stats(anyString(), anyString(), anyString()))
        .thenReturn(SourceData.alertsStats());
  }

  /** Value of the Authorization header for a valid token of the given role. */
  protected static String bearer(String role) {
    return "Bearer " + sign(role, Instant.now().plusSeconds(300));
  }

  /** Value of the Authorization header for a token that expired an hour ago. */
  protected static String expiredBearer(String role) {
    return "Bearer " + sign(role, Instant.now().minusSeconds(3600));
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
