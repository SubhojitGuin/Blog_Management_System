package com.project.Blog_Management_System.Schedulers;

import com.project.Blog_Management_System.Entities.PostEntity;
import com.project.Blog_Management_System.Events.ScheduledPostPublishedEvent;
import com.project.Blog_Management_System.Repositories.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostPublishTask {

    private final PostRepository postRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void execute() {
        LocalDateTime now = LocalDateTime.now();

        List<PostEntity> publishedPosts = postRepository.publishDuePosts(now);

        publishedPosts.forEach(postEntity ->
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
