package com.fleetcontrol.msgateway.exception;

import com.fleetcontrol.msgateway.constants.ErrorConstants;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.PrematureCloseException;

/**
 * Answers the errors of the gateway with the common error contract: {@code 503 SERVICE_UNAVAILABLE}
 * when a downstream service fails (connection refused, unresolvable host, timeouts, closed
 * connections), {@code 404 NOT_FOUND} when no route matches and {@code 500 INTERNAL_ERROR} for
 * unexpected failures.
 *
 * <p>Other HTTP errors raised by Spring (for example a 405) are passed on to the next handler.
 */
@Slf4j
@Component
@Order(-2)
@RequiredArgsConstructor
public class GatewayErrorHandler implements ErrorWebExceptionHandler {

  private static final String ROUTE_ID_SUFFIX = "-route";
  private static final int MAX_CAUSE_DEPTH = 10;

  private final ErrorResponseWriter errorWriter;

  @Override
  public Mono<Void> handle(ServerWebExchange exchange, Throwable throwable) {
    if (exchange.getResponse().isCommitted()) {
      return Mono.error(throwable);
    }
    Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
    if (route != null && isDownstreamFailure(throwable)) {
      return serviceUnavailable(exchange, serviceName(route.getId()), throwable);
    }
    if (throwable instanceof ResponseStatusException exception) {
      if (exception.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
        return errorWriter.write(
            exchange,
            HttpStatus.NOT_FOUND,
            ErrorConstants.NOT_FOUND,
            ErrorConstants.NOT_FOUND_MESSAGE,
            null,
            null);
      }
      return Mono.error(throwable);
    }
    log.error(
        "Unexpected error handling {} {}",
        exchange.getRequest().getMethod(),
        exchange.getRequest().getPath(),
        throwable);
    return errorWriter.write(
        exchange,
        HttpStatus.INTERNAL_SERVER_ERROR,
        ErrorConstants.INTERNAL_ERROR,
        ErrorConstants.INTERNAL_ERROR_MESSAGE,
        null,
        null);
  }

  private Mono<Void> serviceUnavailable(
      ServerWebExchange exchange, String service, Throwable throwable) {
    log.warn(
        "Service {} did not respond to {} {}: {}",
        service,
        exchange.getRequest().getMethod(),
        exchange.getRequest().getPath(),
        throwable.toString());
    return errorWriter.write(
        exchange,
        HttpStatus.SERVICE_UNAVAILABLE,
        ErrorConstants.SERVICE_UNAVAILABLE,
        String.format(ErrorConstants.SERVICE_UNAVAILABLE_MESSAGE, service),
        service,
        null);
  }

  private static String serviceName(String routeId) {
    return routeId.endsWith(ROUTE_ID_SUFFIX)
        ? routeId.substring(0, routeId.length() - ROUTE_ID_SUFFIX.length())
        : routeId;
  }

  private static boolean isDownstreamFailure(Throwable throwable) {
    Throwable current = throwable;
    for (int depth = 0; current != null && depth < MAX_CAUSE_DEPTH; depth++) {
      if (isDownstreamCause(current)) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }

  private static boolean isDownstreamCause(Throwable throwable) {
    if (throwable instanceof ResponseStatusException exception) {
      int status = exception.getStatusCode().value();
      return status == HttpStatus.BAD_GATEWAY.value()
          || status == HttpStatus.SERVICE_UNAVAILABLE.value()
          || status == HttpStatus.GATEWAY_TIMEOUT.value();
    }
    return throwable instanceof SocketException
        || throwable instanceof UnknownHostException
        || throwable instanceof TimeoutException
        || throwable instanceof io.netty.handler.timeout.TimeoutException
        || throwable instanceof PrematureCloseException;
  }
}
