package com.fleetcontrol.msauth.services;

import com.fleetcontrol.msauth.config.properties.JwtProperties;
import com.fleetcontrol.msauth.constants.SecurityConstants;
import com.fleetcontrol.msauth.exceptions.InvalidTokenException;
import com.fleetcontrol.msauth.exceptions.TokenExpiredException;
import com.fleetcontrol.msauth.models.entities.User;
import com.fleetcontrol.msauth.models.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service responsible for issuing and reading JSON Web Tokens (HS256).
 *
 * <p>The token carries the claims {@code sub} (user id), {@code username} and {@code role}, so the
 * other services can validate it locally without calling ms-auth.
 */
@Service
@RequiredArgsConstructor
public class JwtService {

  private final JwtProperties jwt;
  private final SecretKey signingKey;

  /** Data extracted from a valid token. */
  public record TokenData(UUID userId, String username, UserRole role) {}

  /**
   * Generates a signed access token for the given user.
   *
   * @param user the authenticated user
   * @return the compact JWT
   */
  public String generateToken(User user) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(user.getId().toString())
        .claim(SecurityConstants.Claim.USERNAME, user.getUsername())
        .claim(SecurityConstants.Claim.ROLE, user.getRole().name())
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusMillis(jwt.expirationMs())))
        .signWith(signingKey, Jwts.SIG.HS256)
        .compact();
  }

  /**
   * Returns the token lifetime in seconds, as sent in the {@code expiresIn} field.
   *
   * @return the lifetime in seconds
   */
  public long getExpiresInSeconds() {
    return jwt.expirationMs() / 1000;
  }

  /**
   * Verifies the signature and the expiration of a token and extracts its data.
   *
   * @param token the raw JWT, without the {@code Bearer} prefix
   * @return the user id, username and role stored in the token
   * @throws TokenExpiredException if the token is correctly signed but has expired
   * @throws InvalidTokenException if the token is malformed, has a wrong signature or lacks claims
   */
  public TokenData parseToken(String token) {
    Claims claims = readClaims(token);
    try {
      String subject = claims.getSubject();
      String username = claims.get(SecurityConstants.Claim.USERNAME, String.class);
      String role = claims.get(SecurityConstants.Claim.ROLE, String.class);
      if (subject == null || username == null || role == null) {
        throw new InvalidTokenException();
      }
      return new TokenData(UUID.fromString(subject), username, UserRole.valueOf(role));
    } catch (IllegalArgumentException | JwtException e) {
      throw new InvalidTokenException();
    }
  }

  private Claims readClaims(String token) {
    try {
      return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    } catch (ExpiredJwtException e) {
      throw new TokenExpiredException();
    } catch (JwtException | IllegalArgumentException e) {
      throw new InvalidTokenException();
    }
  }
}
