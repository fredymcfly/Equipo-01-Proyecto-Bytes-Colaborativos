package com.fleetcontrol.msauth.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fleetcontrol.msauth.config.properties.JwtProperties;
import com.fleetcontrol.msauth.exceptions.InvalidTokenException;
import com.fleetcontrol.msauth.exceptions.TokenExpiredException;
import com.fleetcontrol.msauth.models.entities.User;
import com.fleetcontrol.msauth.models.enums.UserRole;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

  private static final String SECRET = "test-secret-key-for-ms-auth-tests-0123456789";
  private static final String OTHER_SECRET = "another-secret-key-for-ms-auth-tests-9876543210";

  private final User user =
      User.builder()
          .id(UUID.randomUUID())
          .username("carlos_ruiz")
          .email("carlos@fleetcontrol.com")
          .role(UserRole.MANAGER)
          .build();

  private static SecretKey key(String secret) {
    return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  private static JwtService service(String secret, long expirationMs) {
    return new JwtService(new JwtProperties(secret, "Bearer", expirationMs), key(secret));
  }

  @Test
  void validTokenReturnsTheUserData() {
    JwtService service = service(SECRET, 3_600_000);

    JwtService.TokenData data = service.parseToken(service.generateToken(user));

    assertThat(data.userId()).isEqualTo(user.getId());
    assertThat(data.username()).isEqualTo("carlos_ruiz");
    assertThat(data.role()).isEqualTo(UserRole.MANAGER);
  }

  @Test
  void expiresInIsExpressedInSeconds() {
    assertThat(service(SECRET, 3_600_000).getExpiresInSeconds()).isEqualTo(3600);
  }

  @Test
  void expiredTokenThrowsTokenExpired() {
    JwtService service = service(SECRET, -1_000);

    String token = service.generateToken(user);

    assertThatThrownBy(() -> service.parseToken(token)).isInstanceOf(TokenExpiredException.class);
  }

  @Test
  void tokenSignedWithAnotherKeyThrowsInvalidToken() {
    String token = service(OTHER_SECRET, 3_600_000).generateToken(user);

    assertThatThrownBy(() -> service(SECRET, 3_600_000).parseToken(token))
        .isInstanceOf(InvalidTokenException.class);
  }

  @Test
  void malformedTokenThrowsInvalidToken() {
    JwtService service = service(SECRET, 3_600_000);

    assertThatThrownBy(() -> service.parseToken("not-a-jwt"))
        .isInstanceOf(InvalidTokenException.class);
  }
}
