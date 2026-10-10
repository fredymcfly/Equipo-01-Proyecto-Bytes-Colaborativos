package com.fleetcontrol.msgateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetcontrol.msgateway.config.properties.RateLimitProperties;
import com.fleetcontrol.msgateway.exception.ErrorResponseWriter;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class RateLimitFilterTest {

  private static final int LIMIT = 3;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final AtomicLong nowNanos = new AtomicLong();
  private final AtomicInteger forwarded = new AtomicInteger();
  private final WebFilterChain chain =
      exchange -> {
        forwarded.incrementAndGet();
        return Mono.empty();
      };
  private final RateLimitFilter filter =
      new RateLimitFilter(
          new RateLimitProperties(LIMIT), new ErrorResponseWriter(objectMapper), nowNanos::get);

  private MockServerWebExchange call(String ip) {
    MockServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/vehicles")
                .remoteAddress(new InetSocketAddress(ip, 50000))
                .build());
    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
    return exchange;
  }

  private void advanceSeconds(long seconds) {
    nowNanos.addAndGet(TimeUnit.SECONDS.toNanos(seconds));
  }

  @Test
  void allowsRequestsUpToTheLimit() {
    for (int i = 0; i < LIMIT; i++) {
      MockServerWebExchange exchange = call("10.0.0.1");
      assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    assertThat(forwarded.get()).isEqualTo(LIMIT);
  }

  @Test
  void rejectsRequestsOverTheLimitInContractFormat() throws Exception {
    for (int i = 0; i < LIMIT; i++) {
      call("10.0.0.1");
    }
    advanceSeconds(30);

    MockServerWebExchange rejected = call("10.0.0.1");

    assertThat(forwarded.get()).isEqualTo(LIMIT);
    assertThat(rejected.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    assertThat(rejected.getResponse().getHeaders().getFirst(HttpHeaders.RETRY_AFTER))
        .isEqualTo("30");
    assertThat(rejected.getResponse().getHeaders().getContentType())
        .isEqualTo(MediaType.APPLICATION_JSON);
    JsonNode body = objectMapper.readTree(rejected.getResponse().getBodyAsString().block());
    assertThat(body.get("error").asText()).isEqualTo("RATE_LIMIT_EXCEEDED");
    assertThat(body.get("message").asText()).contains("3 req/min");
    assertThat(body.get("retryAfter").asLong()).isEqualTo(30);
    assertThat(body.has("service")).isFalse();
    assertThat(Instant.parse(body.get("timestamp").asText())).isNotNull();
  }

  @Test
  void countsEachIpSeparately() {
    for (int i = 0; i < LIMIT; i++) {
      call("10.0.0.1");
    }

    MockServerWebExchange otherIp = call("10.0.0.2");

    assertThat(otherIp.getResponse().getStatusCode()).isNull();
    assertThat(forwarded.get()).isEqualTo(LIMIT + 1);
  }

  @Test
  void allowsRequestsAgainOnceTheMinutePasses() {
    for (int i = 0; i < LIMIT + 1; i++) {
      call("10.0.0.1");
    }
    advanceSeconds(61);

    MockServerWebExchange afterWindow = call("10.0.0.1");

    assertThat(afterWindow.getResponse().getStatusCode()).isNull();
    assertThat(forwarded.get()).isEqualTo(LIMIT + 1);
  }

  @Test
  void retryAfterIsAtLeastOneSecond() {
    for (int i = 0; i < LIMIT; i++) {
      call("10.0.0.1");
    }
    nowNanos.addAndGet(TimeUnit.SECONDS.toNanos(60) - 1);

    MockServerWebExchange rejected = call("10.0.0.1");

    assertThat(rejected.getResponse().getHeaders().getFirst(HttpHeaders.RETRY_AFTER))
        .isEqualTo("1");
  }

  @Test
  void rejectsLimitBelowOne() {
    assertThatThrownBy(() -> new RateLimitProperties(0))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
