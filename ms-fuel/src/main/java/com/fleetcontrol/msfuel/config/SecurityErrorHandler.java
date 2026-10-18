package com.fleetcontrol.msfuel.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetcontrol.msfuel.exception.ApiErrorResponse;
import com.fleetcontrol.msfuel.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** Escribe los errores de seguridad 401 y 403 con el contrato de error común. */
@Component
@RequiredArgsConstructor
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

  private final ObjectMapper objectMapper;

  /**
   * Responde 401 TOKEN_EXPIRED si el token ha caducado y 401 UNAUTHORIZED en cualquier otro caso.
   */
  @Override
  public void commence(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
      throws IOException {
    write(response, isExpired(ex) ? ErrorCode.TOKEN_EXPIRED : ErrorCode.UNAUTHORIZED);
  }

  /** Responde 403 FORBIDDEN cuando el rol no tiene permiso para la operación. */
  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException {
    write(response, ErrorCode.FORBIDDEN);
  }

  private boolean isExpired(AuthenticationException ex) {
    return ex instanceof InvalidBearerTokenException
        && ex.getMessage() != null
        && ex.getMessage().toLowerCase(Locale.ROOT).contains("expired");
  }

  private void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
    response.setStatus(errorCode.getStatus().value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    if (errorCode.getStatus().value() == 401) {
      response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
    }
    objectMapper.writeValue(response.getWriter(), ApiErrorResponse.of(errorCode));
  }
}
