package com.project.Blog_Management_System.Schedulers;

import com.project.Blog_Management_System.Entities.PostEntity;
import com.project.Blog_Management_System.Enums.PostStatus;
import com.project.Blog_Management_System.Events.ScheduledPostPublishedEvent;
import com.project.Blog_Management_System.Repositories.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class PostPublishScheduler {

    private final PostRepository postRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(cron = "${blog.schedulers.publishPost.cron:0 * * * * *}")
    public void publishScheduledPosts() {
        LocalDateTime now = LocalDateTime.now();

        List<PostEntity> publishedPosts = postRepository.findByStatusAndPublishAtLessThanEqual(PostStatus.SCHEDULED, now);

        int count = postRepository.publishDuePosts(now);
        if (count > 0) {
            log.info("PostPublishScheduler: published {} scheduled post(s) at {}", count, now);
        }

        publishedPosts
                .forEach(postEntity ->
                            eventPublisher.publishEvent(ScheduledPostPublishedEvent.builder()
                                    .postId(postEntity.getId())
                                    .postSlug(postEntity.getSlug())
                                    .postTitle(postEntity.getTitle())
                                    .authorId(postEntity.getUser().getId())
                                    .authorName(postEntity.getUser().getName())
                                    .authorEmail(postEntity.getUser().getEmail())
                                    .build()
                )
        );
    }
}
