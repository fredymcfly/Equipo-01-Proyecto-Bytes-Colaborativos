package com.fleetcontrol.msauth.security.handlers;

import com.fleetcontrol.msauth.constants.ErrorConstants;
import com.fleetcontrol.msauth.security.writer.ErrorWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/** Answers {@code 401 UNAUTHORIZED} to unauthenticated requests to protected resources. */
@Component
@RequiredArgsConstructor
public class CustomAuthEntryPoint implements AuthenticationEntryPoint {

  private final ErrorWriter writer;

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {
    writer.write(
        response,
        HttpStatus.UNAUTHORIZED,
        ErrorConstants.Code.UNAUTHORIZED,
        ErrorConstants.Message.MISSING_INVALID_TOKEN);
  }
}
