package com.fleetcontrol.msfuel.config;

import jakarta.servlet.DispatcherType;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Protección de ms-fuel.
 *
 * <p>La salud del servicio y la documentación son públicas. El resto exige un JWT HS256 con rol
 * MANAGER o ADMIN. Las consultas (GET) también las pueden hacer otros servicios con la cabecera
 * X-Internal-Key, como ms-alerts y ms-dashboard.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private static final String[] PUBLIC_ENDPOINTS = {
    "/actuator/health",
    "/actuator/health/**",
    "/swagger-ui.html",
    "/swagger-ui/**",
    "/v3/api-docs",
    "/v3/api-docs/**"
  };

  /** Cadena de filtros sin sesión, con JWT y con clave interna para las consultas. */
  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      SecurityErrorHandler errorHandler,
      @Value("${internal.api-key}") String internalApiKey)
      throws Exception {

    // Llamadas internas del contrato: GET /api/fuel/refuels, /consumption y /stats.
    RequestMatcher internalCalls =
        request ->
            HttpMethod.GET.matches(request.getMethod())
                && request.getRequestURI().startsWith("/api/fuel");

    http.csrf(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(
            e -> e.authenticationEntryPoint(errorHandler).accessDeniedHandler(errorHandler))
        .authorizeHttpRequests(
            auth ->
                auth.dispatcherTypeMatchers(DispatcherType.ERROR)
                    .permitAll()
                    .requestMatchers(PUBLIC_ENDPOINTS)
                    .permitAll()
                    .anyRequest()
                    .hasAnyRole("MANAGER", "ADMIN"))
        .oauth2ResourceServer(
            oauth ->
                oauth
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                    .authenticationEntryPoint(errorHandler)
                    .accessDeniedHandler(errorHandler))
        .addFilterBefore(
            new InternalKeyFilter(internalApiKey, internalCalls, errorHandler),
            BearerTokenAuthenticationFilter.class);

    return http.build();
  }

  /** Valida la firma HS256 con el secreto JWT compartido (mínimo 32 caracteres). */
  @Bean
  JwtDecoder jwtDecoder(@Value("${fleet.jwt.secret}") String secret) {
    SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
  }

  private JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
    authorities.setAuthoritiesClaimName("role");
    authorities.setAuthorityPrefix("ROLE_");

    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(authorities);
    return converter;
  }
}
