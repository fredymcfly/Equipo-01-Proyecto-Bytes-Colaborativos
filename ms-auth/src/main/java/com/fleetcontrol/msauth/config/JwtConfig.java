package com.fleetcontrol.msauth.config;

import com.fleetcontrol.msauth.config.properties.JwtProperties;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class responsible for establishing cryptographic keys used in JWT operations.
 *
 * <p>The secret defined in the application properties is used as plain text (UTF-8), so every
 * service must share the same {@code JWT_SECRET} value, with at least 32 characters for HS256.
 */
@Configuration
@RequiredArgsConstructor
public class JwtConfig {

  private final JwtProperties jwt;

  /**
   * Creates and configures a {@link SecretKey} bean for HMAC-SHA signing operations.
   *
   * @return the key used to sign and verify JSON Web Tokens
   * @throws io.jsonwebtoken.security.WeakKeyException if the secret is shorter than 256 bits
   */
  @Bean
  public SecretKey signingKey() {
    return Keys.hmacShaKeyFor(jwt.secret().getBytes(StandardCharsets.UTF_8));
  }
}
