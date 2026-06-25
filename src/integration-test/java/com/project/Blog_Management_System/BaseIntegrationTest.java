package com.project.Blog_Management_System;

import com.redis.testcontainers.RedisContainer;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("integration-test")
public abstract class BaseIntegrationTest {

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("blog_management_system")
            .withUsername("test_user")
            .withPassword("test_pass");

    @Container
    @ServiceConnection
    private static final RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private Flyway flyway;

    @BeforeEach
    void resetTestEnvironmentState() {
        // Reset relational database layout and scripts via Flyway
        flyway.clean();
        flyway.migrate();

        // Flush all active indexes out of the transient Redis container instances
        if (redisTemplate.getConnectionFactory() != null) {
            redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
        }
    }
}
