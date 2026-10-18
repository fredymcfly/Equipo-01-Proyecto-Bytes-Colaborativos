package com.fleetcontrol.msroutes.support;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class of the integration tests: full application on a real PostgreSQL (Testcontainers).
 * Every test class that extends it shares the same context, so the container starts only once.
 *
 * <p>Run without Docker with {@code mvn test -DexcludedGroups=integration}.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestcontainersConfig.class)
@Tag("integration")
public abstract class AbstractIntegrationTest {}
