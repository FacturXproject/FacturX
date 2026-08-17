package com.facturx.app;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Boots the app against a real Postgres via Testcontainers, on a real embedded
 * servlet container (RANDOM_PORT) - MockMvc's default MOCK environment never wires
 * server.servlet.session.cookie.* into Spring Session's cookie serializer.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @DynamicPropertySource
    static void sessionSchemaAlwaysOnTestDb(DynamicPropertyRegistry registry) {
        // Testcontainers' Postgres isn't "embedded" from Spring Session's point of view either -
        // application.properties already forces this, but pin it explicitly so tests don't
        // depend on that file staying in sync.
        registry.add("spring.session.jdbc.initialize-schema", () -> "always");
    }
}
