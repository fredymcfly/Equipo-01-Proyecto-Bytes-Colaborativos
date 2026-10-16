package com.fleetcontrol.msdrivers.security;

import com.fleetcontrol.msdrivers.config.properties.InternalProperties;
import com.fleetcontrol.msdrivers.config.properties.JwtProperties;
import com.fleetcontrol.msdrivers.constants.SecurityConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates a request either with the {@code X-Internal-Key} header or with a JWT.
 *
 * <p>A correct internal key is treated as a MANAGER. A wrong key, or an invalid or expired token,
 * is answered with 401 right away. A request without credentials continues unauthenticated and the
 * security rules decide what to do with it.
 */
public class ApiAuthenticationFilter extends OncePerRequestFilter {

  private final JwtDecoder jwtDecoder;
  private final String tokenPrefix;
  private final byte[] internalKey;
  private final ErrorResponseWriter errorWriter;

  /** Creates the filter with the decoder, the token prefix and the shared internal key. */
  public ApiAuthenticationFilter(
      JwtDecoder jwtDecoder,
      JwtProperties jwtProperties,
      InternalProperties internalProperties,
      ErrorResponseWriter errorWriter) {
    this.jwtDecoder = jwtDecoder;
    String prefix = jwtProperties.tokenPrefix();
    this.tokenPrefix =
        prefix == null || prefix.isBlank() ? SecurityConstants.DEFAULT_TOKEN_PREFIX : prefix;
    String key = internalProperties.apiKey();
    this.internalKey = key == null ? new byte[0] : key.getBytes(StandardCharsets.UTF_8);
    this.errorWriter = errorWriter;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String providedKey = request.getHeader(SecurityConstants.INTERNAL_KEY_HEADER);
    if (providedKey != null) {
      if (!matchesInternalKey(providedKey)) {
        errorWriter.writeUnauthorized(response);
        return;
      }
      authenticate(SecurityConstants.INTERNAL_PRINCIPAL, SecurityConstants.ROLE_MANAGER);
    } else {
      String token = bearerToken(request);
      if (token != null && !authenticateToken(token, response)) {
        return;
      }
    }
    chain.doFilter(request, response);
  }

  private boolean authenticateToken(String token, HttpServletResponse response) throws IOException {
    try {
      Jwt jwt = jwtDecoder.decode(token);
      String role = jwt.getClaimAsString(SecurityConstants.ROLE_CLAIM);
      if (role == null || role.isBlank()) {
        errorWriter.writeUnauthorized(response);
        return false;
      }
      String username = jwt.getClaimAsString(SecurityConstants.USERNAME_CLAIM);
      authenticate(username == null ? jwt.getSubject() : username, role);
      return true;
    } catch (JwtException exception) {
      boolean expired =
          exception.getMessage() != null
              && exception
                  .getMessage()
                  .toLowerCase(Locale.ROOT)
                  .contains(SecurityConstants.EXPIRED_TOKEN_MARKER);
      if (expired) {
        errorWriter.writeTokenExpired(response);
      } else {
        errorWriter.writeUnauthorized(response);
      }
      return false;
    }
  }

  private String bearerToken(HttpServletRequest request) {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    String start = tokenPrefix + " ";
    if (header == null || !header.regionMatches(true, 0, start, 0, start.length())) {
      return null;
    }
    return header.substring(start.length()).trim();
  }

  private boolean matchesInternalKey(String provided) {
    return internalKey.length > 0
        && MessageDigest.isEqual(provided.getBytes(StandardCharsets.UTF_8), internalKey);
  }

  private void authenticate(String principal, String role) {
    List<SimpleGrantedAuthority> authorities =
        List.of(
            new SimpleGrantedAuthority(
                SecurityConstants.ROLE_AUTHORITY_PREFIX + role.toUpperCase(Locale.ROOT)));
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, null, authorities));
    SecurityContextHolder.setContext(context);
  }
}
