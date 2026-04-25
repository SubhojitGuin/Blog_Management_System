package com.project.Blog_Management_System.Schedulers;

import com.project.Blog_Management_System.Repositories.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

import static com.project.Blog_Management_System.Constants.Constants.VIEW_KEY;
import static com.project.Blog_Management_System.Constants.Constants.VIEW_PROCESSING_KEY;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class ViewSyncScheduling {

    private final PostRepository postRepository;
    private final StringRedisTemplate redisTemplate;

    @Scheduled(cron = "${blog.schedulers.viewSync.cron:0 */5 * * * *}")
    public void syncViews() {

        log.info("Starting view count synchronization...");

        Set<String> keys = redisTemplate.keys(VIEW_KEY + "*");

        if (keys == null || keys.isEmpty()) {
            return;
        }

        for (String key : keys) {

            String postId = key.split(":")[3];

            try {
                String processingKey = VIEW_PROCESSING_KEY + postId;
                redisTemplate.renameIfAbsent(key, processingKey);
                String countStr = redisTemplate.opsForValue().get(processingKey);

                if (countStr == null || "0".equals(countStr)) {
                    redisTemplate.delete(processingKey);
                    continue;
                }

                Long count = Long.parseLong(countStr);

                try {
                    postRepository.incrementViewCount(
                            UUID.fromString(postId),
                            count
                    );

                    redisTemplate.delete(processingKey);

                } catch (Exception e) {
                    log.info("Failed to update view count for post {}: {}", postId, e.getMessage());
                }

            } catch (Exception e) {
                log.info("Failed to acquire lock for post {}: {}", postId, e.getMessage());
            }
        }
    }
}
