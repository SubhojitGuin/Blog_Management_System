package com.project.Blog_Management_System.Schedulers;

import com.project.Blog_Management_System.Repositories.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static com.project.Blog_Management_System.Constants.RedisConstants.VIEW_KEY;
import static com.project.Blog_Management_System.Constants.RedisConstants.VIEW_PROCESSING_KEY;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ViewSyncSchedulerTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private ViewSyncScheduler viewSyncScheduler;

    @Nested
    @DisplayName("syncViews()")
    class SyncViews {

        @Test
        @DisplayName("syncs view count for single post to database")
        void syncsViewCountForSinglePostToDatabase() {
            UUID postId = UUID.randomUUID();
            String viewKey = "blog:view:post:" + postId;
            String processingKey = VIEW_PROCESSING_KEY + postId;
            Set<String> keys = new HashSet<>();
            keys.add(viewKey);

            when(redisTemplate.keys(VIEW_KEY + "*")).thenReturn(keys);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(processingKey)).thenReturn("50");

            viewSyncScheduler.syncViews();

            verify(postRepository).incrementViewCount(postId, 50L);
        }

        @Test
        @DisplayName("syncs view counts for multiple posts to database")
        void syncsViewCountsForMultiplePostsToDatabase() {
            UUID postId1 = UUID.randomUUID();
            UUID postId2 = UUID.randomUUID();
            String viewKey1 = "blog:view:post:" + postId1;
            String viewKey2 = "blog:view:post:" + postId2;
            String processingKey1 = VIEW_PROCESSING_KEY + postId1;
            String processingKey2 = VIEW_PROCESSING_KEY + postId2;

            Set<String> keys = new HashSet<>();
            keys.add(viewKey1);
            keys.add(viewKey2);

            when(redisTemplate.keys(VIEW_KEY + "*")).thenReturn(keys);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(processingKey1)).thenReturn("100");
            when(valueOperations.get(processingKey2)).thenReturn("75");

            viewSyncScheduler.syncViews();

            verify(postRepository).incrementViewCount(postId1, 100L);
            verify(postRepository).incrementViewCount(postId2, 75L);
        }

        @Test
        @DisplayName("renames view key to processing key before reading count")
        void renamesViewKeyToProcessingKeyBeforeReadingCount() {
            UUID postId = UUID.randomUUID();
            String viewKey = "blog:view:post:" + postId;
            String processingKey = VIEW_PROCESSING_KEY + postId;
            Set<String> keys = new HashSet<>();
            keys.add(viewKey);

            when(redisTemplate.keys(VIEW_KEY + "*")).thenReturn(keys);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(processingKey)).thenReturn("25");

            viewSyncScheduler.syncViews();

            verify(redisTemplate).renameIfAbsent(viewKey, processingKey);
        }

        @Test
        @DisplayName("skips post with zero view count and deletes processing key")
        void skipsPostWithZeroCountAndDeletesKey() {
            UUID postId = UUID.randomUUID();
            String viewKey = "blog:view:post:" + postId;
            String processingKey = VIEW_PROCESSING_KEY + postId;
            Set<String> keys = new HashSet<>();
            keys.add(viewKey);

            when(redisTemplate.keys(VIEW_KEY + "*")).thenReturn(keys);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(processingKey)).thenReturn("0");

            viewSyncScheduler.syncViews();

            verify(postRepository, never()).incrementViewCount(any(UUID.class), any(Long.class));
            verify(redisTemplate).delete(processingKey);
        }

        @Test
        @DisplayName("skips post with null view count and deletes processing key")
        void skipsPostWithNullCountAndDeletesKey() {
            UUID postId = UUID.randomUUID();
            String viewKey = "blog:view:post:" + postId;
            String processingKey = VIEW_PROCESSING_KEY + postId;
            Set<String> keys = new HashSet<>();
            keys.add(viewKey);

            when(redisTemplate.keys(VIEW_KEY + "*")).thenReturn(keys);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(processingKey)).thenReturn(null);

            viewSyncScheduler.syncViews();

            verify(postRepository, never()).incrementViewCount(any(UUID.class), any(Long.class));
            verify(redisTemplate).delete(processingKey);
        }

        @Test
        @DisplayName("deletes processing key after successful view count update")
        void deletesProcessingKeyAfterSuccessfulUpdate() {
            UUID postId = UUID.randomUUID();
            String viewKey = "blog:view:post:" + postId;
            String processingKey = VIEW_PROCESSING_KEY + postId;
            Set<String> keys = new HashSet<>();
            keys.add(viewKey);

            when(redisTemplate.keys(VIEW_KEY + "*")).thenReturn(keys);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(processingKey)).thenReturn("100");

            viewSyncScheduler.syncViews();

            verify(redisTemplate).delete(processingKey);
        }

        @Test
        @DisplayName("returns early when no view keys exist in Redis")
        void returnsEarlyWhenNoKeysExist() {
            when(redisTemplate.keys(VIEW_KEY + "*")).thenReturn(new HashSet<>());

            viewSyncScheduler.syncViews();

            verify(postRepository, never()).incrementViewCount(any(UUID.class), any(Long.class));
            verify(redisTemplate, never()).delete(any(String.class));
        }

        @Test
        @DisplayName("returns early when keys result is null")
        void returnsEarlyWhenKeysResultIsNull() {
            when(redisTemplate.keys(VIEW_KEY + "*")).thenReturn(null);

            viewSyncScheduler.syncViews();

            verify(postRepository, never()).incrementViewCount(any(UUID.class), any(Long.class));
            verify(redisTemplate, never()).delete(any(String.class));
        }

        @Test
        @DisplayName("extracts correct post ID from Redis key")
        void extractsCorrectPostIdFromRedisKey() {
            UUID postId = UUID.randomUUID();
            String viewKey = "blog:view:post:" + postId;
            String processingKey = VIEW_PROCESSING_KEY + postId;
            Set<String> keys = new HashSet<>();
            keys.add(viewKey);

            when(redisTemplate.keys(VIEW_KEY + "*")).thenReturn(keys);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(processingKey)).thenReturn("10");

            viewSyncScheduler.syncViews();

            ArgumentCaptor<UUID> postIdCaptor = ArgumentCaptor.forClass(UUID.class);
            verify(postRepository).incrementViewCount(postIdCaptor.capture(), eq(10L));

            assertEquals(postId, postIdCaptor.getValue());
        }

        @Test
        @DisplayName("handles database update failure gracefully and logs error")
        void handlesUpdateFailureGracefully() {
            UUID postId = UUID.randomUUID();
            String viewKey = "blog:view:post:" + postId;
            String processingKey = VIEW_PROCESSING_KEY + postId;
            Set<String> keys = new HashSet<>();
            keys.add(viewKey);

            when(redisTemplate.keys(VIEW_KEY + "*")).thenReturn(keys);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(processingKey)).thenReturn("50");
            doThrow(new RuntimeException("Database error"))
                    .when(postRepository).incrementViewCount(any(UUID.class), any(Long.class));

            assertDoesNotThrow(() -> viewSyncScheduler.syncViews());
        }

        @Test
        @DisplayName("continues processing other posts when one fails")
        void continuesProcessingWhenOneFails() {
            UUID postId1 = UUID.randomUUID();
            UUID postId2 = UUID.randomUUID();
            String viewKey1 = "blog:view:post:" + postId1;
            String viewKey2 = "blog:view:post:" + postId2;
            String processingKey1 = VIEW_PROCESSING_KEY + postId1;
            String processingKey2 = VIEW_PROCESSING_KEY + postId2;

            Set<String> keys = new HashSet<>();
            keys.add(viewKey1);
            keys.add(viewKey2);

            when(redisTemplate.keys(VIEW_KEY + "*")).thenReturn(keys);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(processingKey1)).thenReturn("100");
            when(valueOperations.get(processingKey2)).thenReturn("50");
            doThrow(new RuntimeException("Error for post 1"))
                    .when(postRepository).incrementViewCount(postId1, 100L);

            viewSyncScheduler.syncViews();

            verify(postRepository).incrementViewCount(postId2, 50L);
        }

        @Test
        @DisplayName("parses view count as long value correctly")
        void parsesViewCountAsLongCorrectly() {
            UUID postId = UUID.randomUUID();
            String viewKey = "blog:view:post:" + postId;
            String processingKey = VIEW_PROCESSING_KEY + postId;
            Set keys = new HashSet<>();
            keys.add(viewKey);
            when(redisTemplate.keys(VIEW_KEY + "*")).thenReturn(keys);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(processingKey)).thenReturn("999999");
            viewSyncScheduler.syncViews();
            ArgumentCaptor countCaptor = ArgumentCaptor.forClass(Long.class);
            verify(postRepository).incrementViewCount(eq(postId), (Long) countCaptor.capture());
            assertEquals(999999L, countCaptor.getValue());
        }

        @Test
        @DisplayName("queries Redis with correct view key pattern")
        void queriesRedisWithCorrectPattern() {
            when(redisTemplate.keys(VIEW_KEY + "*")).thenReturn(new HashSet<>());
            viewSyncScheduler.syncViews();
            verify(redisTemplate).keys(VIEW_KEY + "*");
        }

        @Test
        @DisplayName("handles large view count values")
        void handlesLargeViewCountValues() {
            UUID postId = UUID.randomUUID();
            String viewKey = "blog:view:post:" + postId;
            String processingKey = VIEW_PROCESSING_KEY + postId;
            Set keys = new HashSet<>();
            keys.add(viewKey);
            Long largeCount = 1000000L;
            when(redisTemplate.keys(VIEW_KEY + "*")).thenReturn(keys);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(processingKey)).thenReturn(largeCount.toString());
            viewSyncScheduler.syncViews();
            ArgumentCaptor countCaptor = ArgumentCaptor.forClass(Long.class);
            verify(postRepository).incrementViewCount(eq(postId), (Long) countCaptor.capture());
            assertEquals(largeCount, countCaptor.getValue());
        }

        @Test
        @DisplayName("handles multiple posts with mixed zero and non-zero counts")
        void handlesMixedZeroAndNonZeroCounts() {
            UUID postId1 = UUID.randomUUID();
            UUID postId2 = UUID.randomUUID();
            UUID postId3 = UUID.randomUUID();
            String viewKey1 = "blog:view:post:" + postId1;
            String viewKey2 = "blog:view:post:" + postId2;
            String viewKey3 = "blog:view:post:" + postId3;
            String processingKey1 = VIEW_PROCESSING_KEY + postId1;
            String processingKey2 = VIEW_PROCESSING_KEY + postId2;
            String processingKey3 = VIEW_PROCESSING_KEY + postId3;
            Set keys = new HashSet<>();
            keys.add(viewKey1);
            keys.add(viewKey2);
            keys.add(viewKey3);
            when(redisTemplate.keys(VIEW_KEY + "*")).thenReturn(keys);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(processingKey1)).thenReturn("100");
            when(valueOperations.get(processingKey2)).thenReturn("0");
            when(valueOperations.get(processingKey3)).thenReturn("50");
            viewSyncScheduler.syncViews();
            verify(postRepository).incrementViewCount(postId1, 100L);
            verify(postRepository, never()).incrementViewCount(postId2, 0L);
            verify(postRepository).incrementViewCount(postId3, 50L);
        }

        @Test
        @DisplayName("handles rename failure gracefully")
        void handlesRenameFailureGracefully() {
            UUID postId = UUID.randomUUID();
            String viewKey = "blog:view:post:" + postId;
            String processingKey = VIEW_PROCESSING_KEY + postId;
            Set keys = new HashSet<>();
            keys.add(viewKey);
            when(redisTemplate.keys(VIEW_KEY + "*")).thenReturn(keys);
            doThrow(new RuntimeException("Rename failed")).when(redisTemplate).renameIfAbsent(viewKey, processingKey);
            assertDoesNotThrow(() -> viewSyncScheduler.syncViews());
            verify(redisTemplate, never()).opsForValue();
        }

        @Test
        @DisplayName("processes all posts even when Redis returns mixed results")
        void processesAllPostsWithMixedResults() {
            UUID postId1 = UUID.randomUUID();
            UUID postId2 = UUID.randomUUID();
            String viewKey1 = "blog:view:post:" + postId1;
            String viewKey2 = "blog:view:post:" + postId2;
            String processingKey1 = VIEW_PROCESSING_KEY + postId1;
            String processingKey2 = VIEW_PROCESSING_KEY + postId2;
            Set keys = new HashSet<>();
            keys.add(viewKey1);
            keys.add(viewKey2);
            when(redisTemplate.keys(VIEW_KEY + "*")).thenReturn(keys);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(processingKey1)).thenReturn("100");
            when(valueOperations.get(processingKey2)).thenReturn(null);
            viewSyncScheduler.syncViews();
            verify(postRepository).incrementViewCount(postId1, 100L);
            verify(redisTemplate, times(2)).delete(any(String.class));
        }
    }
}