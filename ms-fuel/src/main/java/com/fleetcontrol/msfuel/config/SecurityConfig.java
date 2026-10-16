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
 * <p>Solo quedan públicos la salud del servicio, la documentación y los endpoints de repostajes.
 * Estos últimos se abren de momento para poder usarlos y probarlos; cuando llegue el filtro JWT se
 * les exigirá MANAGER o ADMIN quitando la regla que los libera.
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
                    .requestMatchers("/api/fuel/**")
                    .permitAll()
                    .requestMatchers("/error")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .build();
  }
}
