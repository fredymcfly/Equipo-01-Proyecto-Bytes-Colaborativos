package com.fleetcontrol.msauth.security.writer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetcontrol.msauth.models.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Writes the common error contract directly to the HTTP response.
 *
 * <p>The security filters run before Spring MVC, so their errors never reach the {@code
 * GlobalExceptionHandler} and have to be written by hand.
 */
@Component
@RequiredArgsConstructor
public class ErrorWriter {

  private final ObjectMapper objectMapper;

  /**
   * Writes an error response.
   *
   * @param response the response to write to
   * @param status the HTTP status to set
   * @param code the machine-readable code of the {@code error} field
   * @param message the human-readable message
   * @throws IOException if writing to the response fails
   */
  public void write(HttpServletResponse response, HttpStatus status, String code, String message)
      throws IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    objectMapper.writeValue(response.getWriter(), ErrorResponse.of(code, message));
  }
}
