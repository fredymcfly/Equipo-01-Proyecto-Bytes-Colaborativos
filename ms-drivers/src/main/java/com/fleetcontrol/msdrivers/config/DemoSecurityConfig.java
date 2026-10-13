
package com.fleetcontrol.msdrivers.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("demo")
public class DemoSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain demoSecurityFilterChain(
            HttpSecurity http) throws Exception {

        return http
                .securityMatcher("/api/drivers", "/api/drivers/**")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                HttpMethod.POST, "/api/drivers"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.GET, "/api/drivers/*"
                        ).permitAll()
                        .anyRequest().authenticated()

                )
                .build();
    }
}
