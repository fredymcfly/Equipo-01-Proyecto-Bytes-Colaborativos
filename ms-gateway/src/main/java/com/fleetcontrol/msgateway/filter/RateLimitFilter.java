package com.fleetcontrol.msgateway.filter;

import com.fleetcontrol.msgateway.config.properties.RateLimitProperties;
import com.fleetcontrol.msgateway.constants.ErrorConstants;
import com.fleetcontrol.msgateway.exception.ErrorResponseWriter;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Limits the requests each client IP can make per minute, using a fixed one-minute window that
 * starts with the first request of that IP. Requests over the limit get {@code 429
 * RATE_LIMIT_EXCEEDED} with a {@code Retry-After} header.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitFilter implements WebFilter {

  private static final Duration WINDOW = Duration.ofMinutes(1);
  private static final long NANOS_PER_SECOND = 1_000_000_000L;
  private static final long MAX_TRACKED_CLIENTS = 100_000;
  private static final String UNKNOWN_CLIENT = "unknown";

  private final int limit;
  private final Ticker ticker;
  private final ErrorResponseWriter errorWriter;
  private final Cache<String, Window> windows;

  /** Creates the filter with the real clock. */
  @Autowired
  public RateLimitFilter(RateLimitProperties properties, ErrorResponseWriter errorWriter) {
    this(properties, errorWriter, Ticker.systemTicker());
  }

  RateLimitFilter(RateLimitProperties properties, ErrorResponseWriter errorWriter, Ticker ticker) {
    this.limit = properties.requestsPerMinute();
    this.errorWriter = errorWriter;
    this.ticker = ticker;
    this.windows =
        Caffeine.newBuilder()
            .ticker(ticker)
            .expireAfterWrite(WINDOW)
            .maximumSize(MAX_TRACKED_CLIENTS)
            .build();
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    Window window = windows.get(clientIp(exchange), key -> new Window(ticker.read()));
    if (window.requests.incrementAndGet() <= limit) {
      return chain.filter(exchange);
    }
    long remainingNanos = window.startNanos + WINDOW.toNanos() - ticker.read();
    long retryAfter = Math.max(1L, (remainingNanos + NANOS_PER_SECOND - 1) / NANOS_PER_SECOND);
    return tooManyRequests(exchange, retryAfter);
  }

  private Mono<Void> tooManyRequests(ServerWebExchange exchange, long retryAfter) {
    exchange.getResponse().getHeaders().set(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter));
    return errorWriter.write(
        exchange,
        HttpStatus.TOO_MANY_REQUESTS,
        ErrorConstants.RATE_LIMIT_EXCEEDED,
        String.format(ErrorConstants.RATE_LIMIT_EXCEEDED_MESSAGE, limit),
        null,
        retryAfter);
  }

  private static String clientIp(ServerWebExchange exchange) {
    InetSocketAddress remote = exchange.getRequest().getRemoteAddress();
    if (remote == null || remote.getAddress() == null) {
      return UNKNOWN_CLIENT;
    }
    return remote.getAddress().getHostAddress();
  }

  /** Requests counted for one client since its window started. */
  private static final class Window {
    private final long startNanos;
    private final AtomicInteger requests = new AtomicInteger();

    private Window(long startNanos) {
      this.startNanos = startNanos;
    }
  }
}
