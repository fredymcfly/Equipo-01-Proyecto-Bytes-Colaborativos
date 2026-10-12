package com.fleetcontrol.msroutes.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/** Authenticates internal service requests using the X-Internal-Key header. */
public class InternalKeyFilter extends OncePerRequestFilter {

  private static final String HEADER = "X-Internal-Key";

  private final byte[] expectedKey;
  private final RequestMatcher internalCalls;
  private final AuthenticationEntryPoint entryPoint;

  /**
   * Creates the filter.
   *
   * @param apiKey expected value of the X-Internal-Key header
   * @param internalCalls requests that may be authenticated with the internal key
   * @param entryPoint writes the 401 response when the key is wrong
   */
  public InternalKeyFilter(
      String apiKey, RequestMatcher internalCalls, AuthenticationEntryPoint entryPoint) {
    this.expectedKey = apiKey.getBytes(StandardCharsets.UTF_8);
    this.internalCalls = internalCalls;
    this.entryPoint = entryPoint;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return request.getHeader(HEADER) == null || !internalCalls.matches(request);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    byte[] received = request.getHeader(HEADER).getBytes(StandardCharsets.UTF_8);

    if (!MessageDigest.isEqual(received, expectedKey)) {
      entryPoint.commence(request, response, new BadCredentialsException("Invalid internal key"));
      return;
    }
    ;

    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        UsernamePasswordAuthenticationToken.authenticated(
            "internal-service", null, List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))));
    SecurityContextHolder.setContext(context);
    filterChain.doFilter(request, response);
  }
}
