package com.project.Blog_Management_System.Schedulers;

import com.project.Blog_Management_System.Repositories.PostRepository;
import com.project.Blog_Management_System.Utils.AppUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.project.Blog_Management_System.Constants.RedisConstants.VIEW_KEY;
import static com.project.Blog_Management_System.Constants.RedisConstants.VIEW_PROCESSING_KEY;

@Slf4j
@Service
@RequiredArgsConstructor
public class ViewSyncTask {

    private final PostRepository postRepository;
    private final StringRedisTemplate redisTemplate;
    private final AppUtils appUtils;

    @Transactional
    public void execute() {
        log.info("Starting view count synchronization...");

        List<String> keys = appUtils.scanKeys(VIEW_KEY + "*");

        if (keys.isEmpty()) {
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
