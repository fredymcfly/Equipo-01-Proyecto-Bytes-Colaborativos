package com.fleetcontrol.msalerts.config;

import com.fleetcontrol.msalerts.config.properties.InternalProperties;
import com.fleetcontrol.msalerts.config.properties.JwtProperties;
import com.fleetcontrol.msalerts.constants.ApiPaths;
import com.fleetcontrol.msalerts.constants.SecurityConstants;
import com.fleetcontrol.msalerts.security.ApiAuthenticationFilter;
import com.fleetcontrol.msalerts.security.ErrorResponseWriter;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
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

/** Stateless security: JWT of a MANAGER or an ADMIN, and the internal key for some reads. */
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
   * Rules per endpoint. Listing alerts and the statistics are also open to other services (the
   * dashboard reads them with the internal key). Acknowledging and reading the rules need a
   * MANAGER or an ADMIN. Changing a rule and launching an evaluation need an ADMIN. The specific
   * rules go first because the first matching rule wins.
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
                    .requestMatchers(
                        HttpMethod.GET, ApiPaths.Alerts.BASE, ApiPaths.Alerts.FULL_STATS)
                    .hasAnyRole(
                        SecurityConstants.ROLE_MANAGER,
                        SecurityConstants.ROLE_ADMIN,
                        SecurityConstants.ROLE_INTERNAL)
                    .requestMatchers(HttpMethod.PUT, ApiPaths.Alerts.FULL_RULES_ALL)
                    .hasRole(SecurityConstants.ROLE_ADMIN)
                    .requestMatchers(HttpMethod.POST, ApiPaths.Alerts.FULL_EVALUATE)
                    .hasRole(SecurityConstants.ROLE_ADMIN)
                    .requestMatchers(ApiPaths.Alerts.ALL)
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
