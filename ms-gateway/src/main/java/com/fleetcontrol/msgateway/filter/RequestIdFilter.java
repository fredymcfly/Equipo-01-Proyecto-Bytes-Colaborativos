package com.fleetcontrol.msgateway.filter;

import com.fleetcontrol.msgateway.constants.HeaderConstants;
import java.util.UUID;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Adds a generated {@code X-Request-Id} to the requests that arrive without one or with a value
 * that is not a safe id, so clients cannot put arbitrary text into the logs.
 */
@Component
public class RequestIdFilter implements GlobalFilter, Ordered {

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String current = exchange.getRequest().getHeaders().getFirst(HeaderConstants.X_REQUEST_ID);
    if (current != null && HeaderConstants.VALID_REQUEST_ID.matcher(current).matches()) {
      return chain.filter(exchange);
    }
    ServerWebExchange withId =
        exchange
            .mutate()
            .request(
                request ->
                    request.headers(
                        headers ->
                            headers.set(
                                HeaderConstants.X_REQUEST_ID, UUID.randomUUID().toString())))
            .build();
    return chain.filter(withId);
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 1;
  }
}
