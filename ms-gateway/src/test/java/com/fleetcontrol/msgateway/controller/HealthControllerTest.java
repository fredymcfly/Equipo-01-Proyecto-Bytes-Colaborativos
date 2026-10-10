package com.fleetcontrol.msgateway.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fleetcontrol.msgateway.dto.HealthResponse;
import com.fleetcontrol.msgateway.service.HealthService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

class HealthControllerTest {

  @Test
  void returnsTheHealthOfTheGatewayAndItsServices() {
    Map<String, String> services = new LinkedHashMap<>();
    services.put("ms-auth", "UP");
    services.put("ms-dashboard", "DOWN");
    HealthService healthService = mock(HealthService.class);
    when(healthService.check())
        .thenReturn(Mono.just(new HealthResponse("UP", "2026-10-05T10:30:00Z", services)));
    WebTestClient client =
        WebTestClient.bindToController(new HealthController(healthService)).build();

    client
        .get()
        .uri("/health")
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.gateway")
        .isEqualTo("UP")
        .jsonPath("$.timestamp")
        .isEqualTo("2026-10-05T10:30:00Z")
        .jsonPath("$.services.ms-auth")
        .isEqualTo("UP")
        .jsonPath("$.services.ms-dashboard")
        .isEqualTo("DOWN");
  }
}
