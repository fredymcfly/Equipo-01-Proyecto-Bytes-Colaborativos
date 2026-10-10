package com.fleetcontrol.msgateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetcontrol.msgateway.dto.ErrorResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Writes the error body of the common contract as JSON for the errors the gateway creates. */
@Component
@RequiredArgsConstructor
public class ErrorResponseWriter {

  private final ObjectMapper objectMapper;

  /**
   * Answers the exchange with the given status and an {@link ErrorResponse} body that carries the
   * current timestamp.
   *
   * @param service name of the failing service, or {@code null} when it does not apply
   * @param retryAfter seconds until the client can retry, or {@code null} when it does not apply
   */
  public Mono<Void> write(
      ServerWebExchange exchange,
      HttpStatus status,
      String error,
      String message,
      String service,
      Long retryAfter) {
    ErrorResponse body =
        new ErrorResponse(
            error,
            message,
            service,
            retryAfter,
            Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());
    byte[] bytes;
    try {
      bytes = objectMapper.writeValueAsBytes(body);
    } catch (JsonProcessingException e) {
      return Mono.error(e);
    }
    ServerHttpResponse response = exchange.getResponse();
    response.setStatusCode(status);
    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
  }
}
