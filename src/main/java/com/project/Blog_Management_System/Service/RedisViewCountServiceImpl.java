package com.project.Blog_Management_System.Service;

import com.project.Blog_Management_System.Service.Interfaces.RedisViewCountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.project.Blog_Management_System.Constants.Constants.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisViewCountServiceImpl implements RedisViewCountService {

    private final StringRedisTemplate redisTemplate;

    public void addViewer(UUID postId, UUID userId) {
        String uniqueViewKey = UNIQUE_VIEW_KEY + postId;
        Long result = redisTemplate.opsForSet().add(uniqueViewKey, String.valueOf(userId));
        boolean isNew = result != null && result == 1L;
        if (isNew) {
            redisTemplate.opsForValue().increment(VIEW_KEY + postId);
            Long ttl = redisTemplate.getExpire(uniqueViewKey);

            if (ttl == null || ttl <= 0) {
                redisTemplate.expire(uniqueViewKey, TTL, TimeUnit.HOURS);
            }
        }
    }
}
