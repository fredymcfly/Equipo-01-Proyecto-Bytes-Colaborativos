package com.fleetcontrol.msauth.security.handlers;

import com.fleetcontrol.msauth.constants.ErrorConstants;
import com.fleetcontrol.msauth.security.writer.ErrorWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** Answers {@code 403 FORBIDDEN} to authenticated users without enough permissions. */
@Component
@RequiredArgsConstructor
public class CustomAccessDenied implements AccessDeniedHandler {

  private final ErrorWriter writer;

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException {
    writer.write(
        response,
        HttpStatus.FORBIDDEN,
        ErrorConstants.Code.FORBIDDEN,
        ErrorConstants.Message.ACCESS_DENIED);
  }
}
