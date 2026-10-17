package com.fleetcontrol.msalerts.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Starts a PostgreSQL container and wires it as the datasource. Flyway runs the real migrations on
 * it, so the tests exercise the same schema as production.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestcontainersConfig {

  /** PostgreSQL container shared by every test that uses the same Spring context. */
  @Bean
  @ServiceConnection
  PostgreSQLContainer<?> postgres() {
    return new PostgreSQLContainer<>("postgres:16-alpine");
  }
}
