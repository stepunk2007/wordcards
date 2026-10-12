package dev.words.infra.db;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base class for repository tests against a real PostgreSQL with Flyway migrations applied.
 * Each test runs in a transaction that is rolled back.
 */
@SpringBootTest(classes = RepositoryTestApplication.class)
@Transactional
abstract class RepositoryTest {

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18");

    static {
        POSTGRES.start();
    }
}
