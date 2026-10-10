package com.fleetcontrol.msgateway.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fleetcontrol.msgateway.constants.HeaderConstants;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Starts the whole gateway on a random port, with ms-vehicles replaced by a fake HTTP server, to
 * check what a request really looks like when it reaches the downstream service.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayProxyIntegrationTest {

  private static final StubService VEHICLES = new StubService();
  private static final String OK_BODY = "{\"id\":\"42\",\"plate\":\"1234ABC\"}";

  @Autowired private WebTestClient client;

  @DynamicPropertySource
  static void serviceUrls(DynamicPropertyRegistry registry) {
    registry.add("fleet.gateway.service.vehicles", VEHICLES::baseUrl);
  }

  @AfterAll
  static void stopStub() {
    VEHICLES.stop();
  }

  @BeforeEach
  void resetStub() {
    VEHICLES.respondWith(200, OK_BODY);
  }

  @Test
  void forwardsPathAndQueryUnchangedAndReturnsTheResponse() {
    client
        .get()
        .uri("/api/vehicles/42/status?page=1&size=5")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .isEqualTo(OK_BODY);

    Received received = VEHICLES.lastRequest();
    assertThat(received.method()).isEqualTo("GET");
    assertThat(received.uri()).isEqualTo("/api/vehicles/42/status?page=1&size=5");
  }

  @Test
  void forwardsMethodBodyAndAuthorizationHeader() {
    client
        .post()
        .uri("/api/vehicles")
        .contentType(MediaType.APPLICATION_JSON)
        .header(HttpHeaders.AUTHORIZATION, "Bearer token-123")
        .bodyValue("{\"plate\":\"1234ABC\"}")
        .exchange()
        .expectStatus()
        .isOk();

    Received received = VEHICLES.lastRequest();
    assertThat(received.method()).isEqualTo("POST");
    assertThat(received.uri()).isEqualTo("/api/vehicles");
    assertThat(received.body()).isEqualTo("{\"plate\":\"1234ABC\"}");
    assertThat(received.headers()).containsEntry("Authorization", "Bearer token-123");
  }

  @Test
  void passesClientErrorsThroughWithoutChangingThem() {
    String notFound =
        "{\"error\":\"VEHICLE_NOT_FOUND\",\"message\":\"Vehicle 42 not found\","
            + "\"timestamp\":\"2026-10-05T10:30:00Z\"}";
    VEHICLES.respondWith(404, notFound);

    client
        .get()
        .uri("/api/vehicles/42")
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectHeader()
        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
        .expectBody(String.class)
        .isEqualTo(notFound);
  }

  @Test
  void addsRequestIdWhenTheClientSendsNone() {
    client.get().uri("/api/vehicles/42").exchange().expectStatus().isOk();

    String requestId = VEHICLES.lastRequest().headers().get(HeaderConstants.X_REQUEST_ID);
    assertThat(UUID.fromString(requestId)).isNotNull();
  }

  @Test
  void keepsTheRequestIdSentByTheClient() {
    client
        .get()
        .uri("/api/vehicles/42")
        .header(HeaderConstants.X_REQUEST_ID, "client-request-1")
        .exchange()
        .expectStatus()
        .isOk();

    assertThat(VEHICLES.lastRequest().headers())
        .containsEntry(HeaderConstants.X_REQUEST_ID, "client-request-1");
  }

  @Test
  void doesNotForwardTheInternalKeySentByTheClient() {
    client
        .get()
        .uri("/api/vehicles/42")
        .header(HeaderConstants.X_INTERNAL_KEY, "key-from-an-attacker")
        .exchange()
        .expectStatus()
        .isOk();

    assertThat(VEHICLES.lastRequest().headers()).doesNotContainKey(HeaderConstants.X_INTERNAL_KEY);
  }

  @Test
  void answersServiceUnavailableWhenTheServiceIsDown() {
    // Nothing listens on the drivers URL of the test configuration.
    client
        .get()
        .uri("/api/drivers")
        .exchange()
        .expectStatus()
        .isEqualTo(503)
        .expectBody()
        .jsonPath("$.error")
        .isEqualTo("SERVICE_UNAVAILABLE")
        .jsonPath("$.service")
        .isEqualTo("ms-drivers")
        .jsonPath("$.timestamp")
        .isNotEmpty();
  }

  @Test
  void answersNotFoundInContractFormatWhenNoRouteMatches() {
    client
        .get()
        .uri("/api/unknown")
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.error")
        .isEqualTo("NOT_FOUND")
        .jsonPath("$.timestamp")
        .isNotEmpty();
  }

  /** What the fake service saw in the last request. */
  private record Received(String method, String uri, Map<String, String> headers, String body) {}

  /** A tiny HTTP server that records the last request and answers with a fixed response. */
  private static final class StubService {

    private final HttpServer server;
    private final AtomicReference<Received> last = new AtomicReference<>();
    private volatile int status = 200;
    private volatile String body = "{}";

    private StubService() {
      try {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      server.createContext(
          "/",
          exchange -> {
            Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            exchange
                .getRequestHeaders()
                .forEach((name, values) -> headers.put(name, values.get(0)));
            String requestBody =
                new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            last.set(
                new Received(
                    exchange.getRequestMethod(),
                    exchange.getRequestURI().toString(),
                    headers,
                    requestBody));

            byte[] response = body.getBytes(StandardCharsets.UTF_8);
            exchange
                .getResponseHeaders()
                .add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            exchange.sendResponseHeaders(status, response.length);
            try (OutputStream out = exchange.getResponseBody()) {
              out.write(response);
            }
          });
      server.start();
    }

    private String baseUrl() {
      InetSocketAddress address = server.getAddress();
      return "http://" + address.getAddress().getHostAddress() + ":" + address.getPort();
    }

    private void respondWith(int newStatus, String newBody) {
      this.status = newStatus;
      this.body = newBody;
      last.set(null);
    }

    private Received lastRequest() {
      return last.get();
    }

    private void stop() {
      server.stop(0);
    }
  }
}
