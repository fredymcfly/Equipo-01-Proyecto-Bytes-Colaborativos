package com.fleetcontrol.msgateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.fleetcontrol.msgateway.constants.HeaderConstants;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class RequestIdFilterTest {

  private final RequestIdFilter filter = new RequestIdFilter();

  private String forwardedRequestId(MockServerHttpRequest request) {
    AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
    GatewayFilterChain chain =
        next -> {
          forwarded.set(next);
          return Mono.empty();
        };

    StepVerifier.create(filter.filter(MockServerWebExchange.from(request), chain)).verifyComplete();

    return forwarded.get().getRequest().getHeaders().getFirst(HeaderConstants.X_REQUEST_ID);
  }

  @Test
  void generatesRequestIdWhenMissing() {
    String requestId = forwardedRequestId(MockServerHttpRequest.get("/api/vehicles").build());

    assertThat(requestId).isNotBlank();
    assertThat(UUID.fromString(requestId)).isNotNull();
  }

  @Test
  void keepsRequestIdWhenPresent() {
    String requestId =
        forwardedRequestId(
            MockServerHttpRequest.get("/api/vehicles")
                .header(HeaderConstants.X_REQUEST_ID, "client-id-1")
                .build());

    assertThat(requestId).isEqualTo("client-id-1");
  }

  @Test
  void replacesBlankRequestId() {
    String requestId =
        forwardedRequestId(
            MockServerHttpRequest.get("/api/vehicles")
                .header(HeaderConstants.X_REQUEST_ID, "  ")
                .build());

    assertThat(requestId).isNotBlank().isNotEqualTo("  ");
  }

  @Test
  void replacesRequestIdWithUnsafeCharacters() {
    String requestId =
        forwardedRequestId(
            MockServerHttpRequest.get("/api/vehicles")
                .header(HeaderConstants.X_REQUEST_ID, "<script> bad id")
                .build());

    assertThat(UUID.fromString(requestId)).isNotNull();
  }

  @Test
  void replacesRequestIdThatIsTooLong() {
    String requestId =
        forwardedRequestId(
            MockServerHttpRequest.get("/api/vehicles")
                .header(HeaderConstants.X_REQUEST_ID, "a".repeat(65))
                .build());

    assertThat(UUID.fromString(requestId)).isNotNull();
  }
}
