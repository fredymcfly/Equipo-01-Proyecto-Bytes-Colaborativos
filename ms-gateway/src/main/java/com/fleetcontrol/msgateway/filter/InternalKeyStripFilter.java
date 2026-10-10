package com.fleetcontrol.msgateway.filter;

import com.fleetcontrol.msgateway.constants.HeaderConstants;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Removes {@code X-Internal-Key} from every incoming request, so that no external client can pose
 * as an internal service.
 */
@Component
public class InternalKeyStripFilter implements GlobalFilter, Ordered {

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    ServerWebExchange sanitized =
        exchange
            .mutate()
            .request(
                request ->
                    request.headers(headers -> headers.remove(HeaderConstants.X_INTERNAL_KEY)))
            .build();
    return chain.filter(sanitized);
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }
}
