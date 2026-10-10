package com.fleetcontrol.msgateway.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fleetcontrol.msgateway.config.properties.ServiceProperties;
import com.fleetcontrol.msgateway.dto.HealthResponse;
import java.net.ConnectException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class HealthServiceTest {

  private static final List<String> EXPECTED_ORDER =
      List.of(
          "ms-auth",
          "ms-vehicles",
          "ms-drivers",
          "ms-routes",
          "ms-maintenance",
          "ms-fuel",
          "ms-alerts",
          "ms-dashboard");

  private static final ServiceProperties PROPERTIES =
      new ServiceProperties(
          "http://auth",
          "http://routes",
          "http://dashboard",
          "http://vehicles",
          "http://alerts",
          "http://drivers",
          "http://fuel",
          "http://maintenance");

  private static HealthService serviceAnswering(ExchangeFunction exchangeFunction) {
    WebClient webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
    return new HealthService(PROPERTIES, webClient, Duration.ofMillis(200));
  }

  private static Mono<ClientResponse> ok() {
    return Mono.just(ClientResponse.create(HttpStatus.OK).build());
  }

  private static HealthResponse checkAndGet(HealthService service) {
    return service.check().block(Duration.ofSeconds(5));
  }

  @Test
  void reportsEveryServiceUpInTheDocumentedOrder() {
    HealthResponse response = checkAndGet(serviceAnswering(request -> ok()));

    assertThat(response.gateway()).isEqualTo("UP");
    assertThat(Instant.parse(response.timestamp())).isNotNull();
    assertThat(response.services().keySet()).containsExactlyElementsOf(EXPECTED_ORDER);
    assertThat(response.services().values()).containsOnly("UP");
  }

  @Test
  void queriesTheActuatorHealthOfEachService() {
    List<URI> queried = new CopyOnWriteArrayList<>();
    ExchangeFunction function =
        request -> {
          queried.add(request.url());
          return ok();
        };

    checkAndGet(serviceAnswering(function));

    assertThat(queried)
        .extracting(URI::toString)
        .contains("http://vehicles/actuator/health", "http://dashboard/actuator/health")
        .hasSize(8);
  }

  @Test
  void marksAsDownTheServiceThatAnswersWithAnError() {
    ExchangeFunction function =
        request ->
            "dashboard".equals(request.url().getHost())
                ? Mono.just(ClientResponse.create(HttpStatus.SERVICE_UNAVAILABLE).build())
                : ok();

    HealthResponse response = checkAndGet(serviceAnswering(function));

    assertThat(response.services()).containsEntry("ms-dashboard", "DOWN");
    List<String> others = new ArrayList<>(EXPECTED_ORDER);
    others.remove("ms-dashboard");
    others.forEach(name -> assertThat(response.services()).containsEntry(name, "UP"));
  }

  @Test
  void marksAsDownTheServiceThatRefusesTheConnection() {
    ExchangeFunction function =
        request ->
            "auth".equals(request.url().getHost())
                ? Mono.error(new ConnectException("Connection refused"))
                : ok();

    HealthResponse response = checkAndGet(serviceAnswering(function));

    assertThat(response.services()).containsEntry("ms-auth", "DOWN");
    assertThat(response.services()).containsEntry("ms-vehicles", "UP");
  }

  @Test
  void marksAsDownTheServiceThatDoesNotAnswerInTime() {
    ExchangeFunction function =
        (ClientRequest request) -> "fuel".equals(request.url().getHost()) ? Mono.never() : ok();

    HealthResponse response = checkAndGet(serviceAnswering(function));

    assertThat(response.services()).containsEntry("ms-fuel", "DOWN");
    assertThat(response.services()).containsEntry("ms-routes", "UP");
  }

  @Test
  void stillRespondsWhenEveryServiceIsDown() {
    HealthResponse response =
        checkAndGet(serviceAnswering(request -> Mono.error(new ConnectException("down"))));

    assertThat(response.gateway()).isEqualTo("UP");
    assertThat(response.services().values()).containsOnly("DOWN");
  }
}
