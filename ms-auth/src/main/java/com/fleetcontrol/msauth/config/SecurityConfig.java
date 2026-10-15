package com.fleetcontrol.msauth.config;

import com.fleetcontrol.msauth.constants.SecurityConstants;
import com.fleetcontrol.msauth.security.handlers.CustomAccessDenied;
import com.fleetcontrol.msauth.security.handlers.CustomAuthEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security rules of ms-auth: stateless, no CSRF, and only the registration, login, validation,
 * health and Swagger endpoints are public.
 *
 * <p>ms-auth has no protected endpoints, so it does not need the JWT filter that the other services
 * use. {@code /api/auth/validate} reads the token itself.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final CustomAuthEntryPoint authEntryPoint;
  private final CustomAccessDenied accessDenied;
  private final AuthenticationProvider authenticationProvider;

  /**
   * Builds the security filter chain.
   *
   * @param http the builder provided by Spring Security
   * @return the configured chain
   * @throws Exception if the configuration cannot be built
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(SecurityConstants.Path.PUBLIC)
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .authenticationProvider(authenticationProvider)
        .exceptionHandling(
            handling ->
                handling
                    .authenticationEntryPoint(authEntryPoint)
                    .accessDeniedHandler(accessDenied));
    return http.build();
  }
}
