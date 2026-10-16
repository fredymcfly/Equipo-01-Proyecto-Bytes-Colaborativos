package com.fleetcontrol.msvehicles.config;

import com.fleetcontrol.msvehicles.config.properties.InternalProperties;
import com.fleetcontrol.msvehicles.config.properties.JwtProperties;
import com.fleetcontrol.msvehicles.constants.ApiPaths;
import com.fleetcontrol.msvehicles.constants.SecurityConstants;
import com.fleetcontrol.msvehicles.security.ApiAuthenticationFilter;
import com.fleetcontrol.msvehicles.security.ErrorResponseWriter;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/** Stateless security: JWT or internal key, with the role rules of the vehicle endpoints. */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private static final String HMAC_SHA256 = "HmacSHA256";

  private static final String[] PUBLIC_PATHS = {
    ApiPaths.Public.ACTUATOR_HEALTH,
    ApiPaths.Public.ACTUATOR_HEALTH_ALL,
    ApiPaths.Public.SWAGGER_UI_HTML,
    ApiPaths.Public.SWAGGER_UI_ALL,
    ApiPaths.Public.API_DOCS,
    ApiPaths.Public.API_DOCS_ALL
  };

  /** Decoder that verifies the HS256 signature and the expiration of the tokens. */
  @Bean
  public JwtDecoder jwtDecoder(JwtProperties properties) {
    SecretKey key =
        new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
    return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
  }

  /**
   * Every vehicle endpoint requires a MANAGER or an ADMIN, and internal calls count as MANAGER. The
   * rule that only an ADMIN can reactivate an out-of-service vehicle lives in the service.
   */
  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtDecoder jwtDecoder,
      JwtProperties jwtProperties,
      InternalProperties internalProperties,
      ErrorResponseWriter errorWriter)
      throws Exception {
    ApiAuthenticationFilter authenticationFilter =
        new ApiAuthenticationFilter(jwtDecoder, jwtProperties, internalProperties, errorWriter);
    http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            requests ->
                requests
                    .requestMatchers(PUBLIC_PATHS)
                    .permitAll()
                    .requestMatchers(ApiPaths.Vehicles.ALL)
                    .hasAnyRole(SecurityConstants.ROLE_MANAGER, SecurityConstants.ROLE_ADMIN)
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            handling ->
                handling
                    .authenticationEntryPoint(
                        (request, response, exception) -> errorWriter.writeUnauthorized(response))
                    .accessDeniedHandler(
                        (request, response, exception) -> errorWriter.writeForbidden(response)))
        .addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }
}
