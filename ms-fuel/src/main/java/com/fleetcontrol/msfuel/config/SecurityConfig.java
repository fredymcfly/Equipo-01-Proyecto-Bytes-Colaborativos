package com.fleetcontrol.msfuel.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Protección de ms-fuel.
 *
 * <p>Solo quedan públicos la salud del servicio y la documentación. La verificación del JWT entra
 * con los primeros endpoints, junto al filtro que la implemente.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  /** Cadena de filtros sin sesión y con las rutas públicas abiertas. */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http.csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            requests ->
                requests
                    .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**")
                    .permitAll()
                    .requestMatchers("/v3/api-docs", "/v3/api-docs/**")
                    .permitAll()
                    .requestMatchers("/swagger-ui.html", "/swagger-ui/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .build();
  }
}
