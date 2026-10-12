package com.fleetcontrol.msgateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.fleetcontrol.msgateway.constants.HeaderConstants;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class InternalKeyStripFilterTest {

  private final InternalKeyStripFilter filter = new InternalKeyStripFilter();

  @Test
  void removesInternalKeyFromExternalRequests() {
    MockServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/vehicles")
                .header(HeaderConstants.X_INTERNAL_KEY, "secret")
                .build());
    AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
    GatewayFilterChain chain =
        next -> {
          forwarded.set(next);
          return Mono.empty();
        };

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

    assertThat(
            forwarded.get().getRequest().getHeaders().containsKey(HeaderConstants.X_INTERNAL_KEY))
        .isFalse();
  }

  @Test
  void keepsOtherHeadersUntouched() {
    MockServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/vehicles")
                .header("Authorization", "Bearer token")
                .header(HeaderConstants.X_INTERNAL_KEY, "secret")
                .build());
    AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
    GatewayFilterChain chain =
        next -> {
          forwarded.set(next);
          return Mono.empty();
        };

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

    assertThat(forwarded.get().getRequest().getHeaders().getFirst("Authorization"))
        .isEqualTo("Bearer token");
  }
}
