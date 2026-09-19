package com.facturx.app;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer postgres =
        new PostgreSQLContainer("postgres:17");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void configureTestDatabase(
            DynamicPropertyRegistry registry) {

        registry.add(
            "spring.session.jdbc.initialize-schema",
            () -> "always"
        );

        registry.add(
            "spring.jpa.hibernate.ddl-auto",
            () -> "create-drop"
        );
    }
}