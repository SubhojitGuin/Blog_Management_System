package com.project.Blog_Management_System;

import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@Feature("Smoke Test")
@Severity(SeverityLevel.CRITICAL)
public class SmokeIT extends BaseIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    @Story("Load and check test containers' health")
    void contextLoadsAndContainersAreHealthy() {
        // Verify Application Context Booted
        assertThat(mockMvc).isNotNull();
        assertThat(objectMapper).isNotNull();

        //Verify PostgreSQL connectivity and read/write access
        Integer dbCheck = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        assertThat(dbCheck).isEqualTo(1);

        // Verify Redis connectivity and read/write access
        redisTemplate.opsForValue().set("smoke_test_key", "working");
        String redisCheck = redisTemplate.opsForValue().get("smoke_test_key");
        assertThat(redisCheck).isEqualTo("working");
    }
}

