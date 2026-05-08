package com.project.Blog_Management_System.Schedulers;

import com.project.Blog_Management_System.Repositories.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class PostPublishScheduler {

    private final PostRepository postRepository;

    @Scheduled(cron = "${blog.schedulers.publishPost.cron:0 * * * * *}")
    public void publishScheduledPosts() {
        LocalDateTime now = LocalDateTime.now();
        int count = postRepository.publishDuePosts(now);

        if (count > 0) {
            log.info("PostPublishScheduler: published {} scheduled post(s) at {}", count, now);
        }
    }
}
