package com.project.Blog_Management_System.Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.project.Blog_Management_System.Constants.RedisConstants.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisViewCountServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RedisViewCountServiceImpl redisViewCountService;

    private UUID postId;
    private UUID userId;
    private String uniqueViewKey;
    private String viewKey;

    @BeforeEach
    void setUp() {
        postId = UUID.randomUUID();
        userId = UUID.randomUUID();
        uniqueViewKey = UNIQUE_VIEW_KEY + postId;
        viewKey = VIEW_KEY + postId;

        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("addViewer(UUID, UUID)")
    class AddViewer {

        @Test
        @DisplayName("increment view count and set TTL when user is a new viewer and TTL does not exist")
        void addViewer_NewViewer_NoTTL_IncrementsAndSetsTTL() {
            when(setOperations.add(uniqueViewKey, String.valueOf(userId))).thenReturn(1L);
            when(redisTemplate.getExpire(uniqueViewKey)).thenReturn(-1L);

            redisViewCountService.addViewer(postId, userId);

            verify(setOperations).add(uniqueViewKey, String.valueOf(userId));
            verify(valueOperations).increment(viewKey);
            verify(redisTemplate).getExpire(uniqueViewKey);
            verify(redisTemplate).expire(uniqueViewKey, TTL, TimeUnit.HOURS);
        }

        @Test
        @DisplayName("increment view count but NOT set TTL when user is a new viewer and TTL already exists")
        void addViewer_NewViewer_WithExistingTTL_OnlyIncrements() {
            when(setOperations.add(uniqueViewKey, String.valueOf(userId))).thenReturn(1L);
            when(redisTemplate.getExpire(uniqueViewKey)).thenReturn(50L);

            redisViewCountService.addViewer(postId, userId);

            verify(setOperations).add(uniqueViewKey, String.valueOf(userId));
            verify(valueOperations).increment(viewKey);
            verify(redisTemplate).getExpire(uniqueViewKey);
            verify(redisTemplate, never()).expire(anyString(), anyLong(), any());
        }

        @Test
        @DisplayName("do nothing when user has already viewed the post")
        void addViewer_ExistingViewer_DoesNothing() {
            when(setOperations.add(uniqueViewKey, String.valueOf(userId))).thenReturn(0L);

            redisViewCountService.addViewer(postId, userId);

            verify(setOperations).add(uniqueViewKey, String.valueOf(userId));
            verify(valueOperations, never()).increment(anyString());
            verify(redisTemplate, never()).getExpire(anyString());
            verify(redisTemplate, never()).expire(anyString(), anyLong(), any());
        }
    }
}
