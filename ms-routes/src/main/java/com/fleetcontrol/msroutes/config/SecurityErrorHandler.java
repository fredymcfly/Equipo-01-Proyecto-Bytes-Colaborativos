package com.fleetcontrol.msroutes.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetcontrol.msroutes.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** Writes the 401 and 403 security errors using the common API error contract. */
@Component
@RequiredArgsConstructor
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

  private final ObjectMapper objectMapper;

  /**
   * Response 401 TOKEN_EXPIRED or expired tokens and 401 UNAUTHORIZED for anything else.
   *
   * @param request that resulted in an <code>AuthenticationException</code>
   * @param response so that the user agent can begin authentication
   * @param ex that caused the invocation
   */
  @Override
  public void commence(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
      throws IOException {
    if (isExpired(ex)) {
      write(response, HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", "The token has expired");
    } else {
      write(
          response,
          HttpStatus.UNAUTHORIZED,
          "UNAUTHORIZED",
          "A valid authentication token is required");
    }
  }

  /**
   * Response 403 FORBIDDEN when the role has no permission for the operation.
   *
   * @param request that resulted in an <code>AccessDeniedException</code>
   * @param response so that the user agent can be advised of the failure
   * @param accessDeniedException that caused the invocation
   */
  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException {
    write(
        response,
        HttpStatus.FORBIDDEN,
        "FORBIDDEN",
        "The user's role does not have permission for this operation");
  }

  private boolean isExpired(AuthenticationException ex) {
    return ex instanceof InvalidBearerTokenException
        && ex.getMessage() != null
        && ex.getMessage().toLowerCase().contains("expired");
  }

  private void write(HttpServletResponse response, HttpStatus status, String code, String message)
      throws IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    if (status == HttpStatus.UNAUTHORIZED) {
      response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
    }
    objectMapper.writeValue(response.getWriter(), ApiError.of(code, message, null, null));
  }
}
