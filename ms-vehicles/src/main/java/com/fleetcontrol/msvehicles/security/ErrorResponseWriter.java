package com.fleetcontrol.msvehicles.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetcontrol.msvehicles.constants.ErrorConstants;
import com.fleetcontrol.msvehicles.models.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/** Writes error contract bodies from places that run before the controllers, such as filters. */
@Component
@RequiredArgsConstructor
public class ErrorResponseWriter {

  private final ObjectMapper objectMapper;

  /** Answers 401 because the request has no valid credentials. */
  public void writeUnauthorized(HttpServletResponse response) throws IOException {
    write(
        response,
        HttpStatus.UNAUTHORIZED,
        ErrorConstants.Code.UNAUTHORIZED,
        ErrorConstants.Message.UNAUTHORIZED);
  }

  /** Answers 401 because the token has expired. */
  public void writeTokenExpired(HttpServletResponse response) throws IOException {
    write(
        response,
        HttpStatus.UNAUTHORIZED,
        ErrorConstants.Code.TOKEN_EXPIRED,
        ErrorConstants.Message.TOKEN_EXPIRED);
  }

  /** Answers 403 because the role cannot use the endpoint. */
  public void writeForbidden(HttpServletResponse response) throws IOException {
    write(
        response,
        HttpStatus.FORBIDDEN,
        ErrorConstants.Code.FORBIDDEN,
        ErrorConstants.Message.FORBIDDEN);
  }

  private void write(HttpServletResponse response, HttpStatus status, String code, String message)
      throws IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    ErrorResponse body = new ErrorResponse(code, message, null, null, Instant.now());
    objectMapper.writeValue(response.getWriter(), body);
  }
}
