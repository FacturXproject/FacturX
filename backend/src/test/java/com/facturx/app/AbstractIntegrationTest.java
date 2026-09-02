package com.facturx.app;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Boots the app against a real Postgres via Testcontainers, on a real embedded
 * servlet container (RANDOM_PORT) - MockMvc's default MOCK environment never wires
 * server.servlet.session.cookie.* into Spring Session's cookie serializer.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void sessionSchemaAlwaysOnTestDb(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.session.jdbc.initialize-schema",
                () -> "always"
        );
    }
}