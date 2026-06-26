package com.project.Blog_Management_System;

import com.project.Blog_Management_System.Utils.TestDataFactory;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Objects;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
public abstract class BaseIntegrationTest {


    @ServiceConnection
    protected static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("blog_management_system")
            .withUsername("test_user")
            .withPassword("test_pass");

    @ServiceConnection
    protected static final RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    static {
        postgres.start();
        redis.start();
    }

    @Autowired
    protected TestDataFactory dataFactory;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected StringRedisTemplate redisTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate; // Introduced for fast truncation

    @BeforeEach
    void resetTestEnvironmentState() {
        // FAST: Clean user tables without dropping schema or re-running Flyway
        truncateRelationalDatabase();

        // SAFE: Wipe Redis without risking connection leaks
        Objects.requireNonNull(redisTemplate.getConnectionFactory())
                .getConnection()
                .serverCommands()
                .flushDb();
    }

    /**
     * Dynamically queries all user tables and truncates them in one single, high-speed pass.
     * Keeps 'flyway_schema_history' intact so migrations only run ONCE at application startup.
     */
    private void truncateRelationalDatabase() {
        List<String> tables = jdbcTemplate.queryForList(
            "SELECT table_name FROM information_schema.tables " +
            "WHERE table_schema = 'public' AND table_name <> 'flyway_schema_history'",
            String.class
        );

        if (!tables.isEmpty()) {
            String truncateQuery = "TRUNCATE TABLE " + String.join(", ", tables) + " RESTART IDENTITY CASCADE";
            jdbcTemplate.execute(truncateQuery);

            String addUncategorisedCategoryQuery = """
                    INSERT INTO CATEGORIES(ID, DESCRIPTION, NAME, SLUG, CREATED_AT, UPDATED_AT)
                    VALUES ('019def21-0c08-71a0-94fa-7752a369d39a', 'Default category', 'Uncategorised', 'uncategorised',
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    ON CONFLICT (ID) DO NOTHING""";

            jdbcTemplate.execute(addUncategorisedCategoryQuery);
        }
    }
}

