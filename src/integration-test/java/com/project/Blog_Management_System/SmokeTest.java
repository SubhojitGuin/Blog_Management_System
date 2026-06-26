package com.project.Blog_Management_System;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

public class SmokeTest extends BaseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
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

