package com.fleetcontrol.msgateway.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import reactor.test.StepVerifier;

class GatewayErrorHandlerTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final GatewayErrorHandler handler =
      new GatewayErrorHandler(new ErrorResponseWriter(objectMapper));

  private static MockServerWebExchange exchangeWithoutRoute() {
    return MockServerWebExchange.from(MockServerHttpRequest.get("/api/vehicles").build());
  }

  private static MockServerWebExchange exchangeWithRoute(String routeId) {
    MockServerWebExchange exchange = exchangeWithoutRoute();
    Route route =
        Route.async().id(routeId).uri("http://localhost:18082").predicate(e -> true).build();
    exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, route);
    return exchange;
  }

  private JsonNode bodyOf(MockServerWebExchange exchange) throws Exception {
    return objectMapper.readTree(exchange.getResponse().getBodyAsString().block());
  }

  @Test
  void connectionRefusedReturnsServiceUnavailableInContractFormat() throws Exception {
    MockServerWebExchange exchange = exchangeWithRoute("ms-vehicles-route");

    StepVerifier.create(handler.handle(exchange, new ConnectException("Connection refused")))
        .verifyComplete();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(exchange.getResponse().getHeaders().getContentType())
        .isEqualTo(MediaType.APPLICATION_JSON);
    JsonNode body = bodyOf(exchange);
    assertThat(body.get("error").asText()).isEqualTo("SERVICE_UNAVAILABLE");
    assertThat(body.get("service").asText()).isEqualTo("ms-vehicles");
    assertThat(body.get("message").asText()).contains("ms-vehicles");
    assertThat(Instant.parse(body.get("timestamp").asText())).isNotNull();
  }

  @ParameterizedTest
  @CsvSource({
    "ms-auth-route,ms-auth",
    "ms-vehicles-route,ms-vehicles",
    "ms-drivers-route,ms-drivers",
    "ms-routes-route,ms-routes",
    "ms-maintenance-route,ms-maintenance",
    "ms-fuel-route,ms-fuel",
    "ms-alerts-route,ms-alerts",
    "ms-dashboard-route,ms-dashboard"
  })
  void reportsTheServiceBehindTheRoute(String routeId, String expectedService) throws Exception {
    MockServerWebExchange exchange = exchangeWithRoute(routeId);

    StepVerifier.create(handler.handle(exchange, new ConnectException())).verifyComplete();

    assertThat(bodyOf(exchange).get("service").asText()).isEqualTo(expectedService);
  }

  @Test
  void unresolvableHostWhenTheContainerIsStoppedReturnsServiceUnavailable() throws Exception {
    MockServerWebExchange exchange = exchangeWithRoute("ms-vehicles-route");
    Throwable cause = new UnknownHostException("ms-vehicles: Name or service not known");

    StepVerifier.create(handler.handle(exchange, new IllegalStateException("wrapper", cause)))
        .verifyComplete();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(bodyOf(exchange).get("service").asText()).isEqualTo("ms-vehicles");
  }

  @Test
  void gatewayTimeoutIsReportedAsServiceUnavailable() {
    MockServerWebExchange exchange = exchangeWithRoute("ms-fuel-route");

    StepVerifier.create(
            handler.handle(exchange, new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT)))
        .verifyComplete();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
  }

  @Test
  void timeoutHiddenInTheCauseChainIsDetected() {
    MockServerWebExchange exchange = exchangeWithRoute("ms-routes-route");
    Throwable wrapped = new IllegalStateException("wrapper", new TimeoutException("5s"));

    StepVerifier.create(handler.handle(exchange, wrapped)).verifyComplete();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
  }

  @Test
  void unexpectedErrorsReturnInternalErrorWithoutLeakingDetails() throws Exception {
    MockServerWebExchange exchange = exchangeWithRoute("ms-auth-route");

    StepVerifier.create(handler.handle(exchange, new IllegalStateException("secret detail")))
        .verifyComplete();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    JsonNode body = bodyOf(exchange);
    assertThat(body.get("error").asText()).isEqualTo("INTERNAL_ERROR");
    assertThat(body.get("message").asText()).doesNotContain("secret");
    assertThat(body.has("service")).isFalse();
    assertThat(Instant.parse(body.get("timestamp").asText())).isNotNull();
  }

  @Test
  void downstreamErrorsWithoutMatchedRouteAreInternalErrors() {
    MockServerWebExchange exchange = exchangeWithoutRoute();

    StepVerifier.create(handler.handle(exchange, new ConnectException())).verifyComplete();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @Test
  void unknownPathReturnsNotFoundInContractFormat() throws Exception {
    MockServerWebExchange exchange = exchangeWithoutRoute();

    StepVerifier.create(handler.handle(exchange, new ResponseStatusException(HttpStatus.NOT_FOUND)))
        .verifyComplete();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    JsonNode body = bodyOf(exchange);
    assertThat(body.get("error").asText()).isEqualTo("NOT_FOUND");
    assertThat(body.get("message").asText()).isNotBlank();
    assertThat(Instant.parse(body.get("timestamp").asText())).isNotNull();
  }

  @Test
  void otherHttpErrorsRaisedBySpringArePassedOn() {
    MockServerWebExchange exchange = exchangeWithoutRoute();

    StepVerifier.create(
            handler.handle(exchange, new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED)))
        .expectError(ResponseStatusException.class)
        .verify();
  }
}
