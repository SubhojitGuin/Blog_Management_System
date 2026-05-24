package com.project.Blog_Management_System.Listeners;

import com.project.Blog_Management_System.Dto.EmailMessageDTO;
import com.project.Blog_Management_System.Entities.FollowEntity;
import com.project.Blog_Management_System.Events.*;
import com.project.Blog_Management_System.Repositories.FollowRepository;
import com.project.Blog_Management_System.Service.Interfaces.EmailService;
import com.project.Blog_Management_System.Service.Interfaces.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailEventListeners {

    @Value("${app.base.url}")
    private String baseUrl;

    private final EmailService emailService;
    private final FollowRepository followRepository;
    private final EmailTemplateService emailTemplateService;

    @Async("emailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNewPostPublished(NewPostPublishedEvent event) {
        List<FollowEntity> followersList = followRepository.findByFollowingId(event.authorId());
        String postUrl = baseUrl + "/posts/" + event.postSlug() + "-" + event.postId();

        followersList.stream()
                .map(FollowEntity::getFollower)
                .forEach(follower -> {
                    try {
                        EmailMessageDTO emailMessageDTO = emailTemplateService.buildNewPostByAuthor(
                                follower.getName(),
                                event.authorName(),
                                event.postTitle(),
                                postUrl
                        );
                        emailService.sendEmail(follower.getEmail(), emailMessageDTO.getSubject(), emailMessageDTO.getBody());
                    } catch (Exception e) {
                        log.error("Failed to send new post email to follower: {}. Error: {}",
                                follower.getName(), e.getMessage());
                    }
                });

    }

    @Async("emailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleScheduledPostPublishedEvent(ScheduledPostPublishedEvent event) {
        String postUrl = baseUrl + "/posts/" + event.postSlug() + "-" + event.postId();

        try {
            EmailMessageDTO ownerMessageDTO = emailTemplateService.buildPostPublished(event.authorName(), event.postTitle(), postUrl);
            emailService.sendEmail(event.authorEmail(), ownerMessageDTO.getSubject(), ownerMessageDTO.getBody());
        } catch (Exception e) {
            log.error("Failed to send scheduled post published email to author: {}. Error: {}",
                    event.authorName(), e.getMessage());
        }

        List<FollowEntity> followersList = followRepository.findByFollowingId(event.authorId());

        followersList.stream()
                .map(FollowEntity::getFollower)
                .forEach(follower -> {
                    try {
                        EmailMessageDTO emailMessageDTO = emailTemplateService.buildNewPostByAuthor(
                                follower.getName(),
                                event.authorName(),
                                event.postTitle(),
                                postUrl
                        );
                        emailService.sendEmail(follower.getEmail(), emailMessageDTO.getSubject(), emailMessageDTO.getBody());
                    } catch (Exception e) {
                        log.error("Failed to send scheduled post published email to follower: {}. Error: {}",
                                follower.getName(), e.getMessage());
                    }
                });
    }

    @Async("emailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCommentAddedEvent(CommentAddedEvent event) {
        String postUrl = baseUrl + "/posts/" + event.postSlug() + "-" + event.postId();

        try {
            EmailMessageDTO emailMessageDTO = emailTemplateService.buildPostCommented(
                    event.authorName(),
                    event.commenterName(),
                    event.postTitle(),
                    postUrl,
                    event.commentSnippet()
            );

            emailService.sendEmail(event.authorEmail(), emailMessageDTO.getSubject(), emailMessageDTO.getBody());
        } catch (Exception e) {
            log.error("Failed to send comment notification email to author: {}. Error: {}",
                    event.authorName(), e.getMessage());
        }
    }

    @Async("emailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCommentRepliedEvent(CommentRepliedEvent event) {
        String postUrl = baseUrl + "/posts/" + event.postSlug() + "-" + event.postId();

        try {
            EmailMessageDTO emailMessageDTO = emailTemplateService.buildCommentReplied(
                    event.parentCommenterName(),
                    event.childCommenterName(),
                    event.authorName(),
                    event.postTitle(),
                    event.originalCommentSnippet(),
                    event.replyCommentSnippet(),
                    postUrl
            );

            emailService.sendEmail(event.parentCommenterEmail(), emailMessageDTO.getSubject(), emailMessageDTO.getBody());
        } catch (Exception e) {
            log.error("Failed to send comment reply notification email to parent commenter: {}. Error: {}",
                    event.parentCommenterName(), e.getMessage());
        }
    }

    @Async("emailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePostLikedEvent(PostLikedEvent event) {
        String postUrl = baseUrl + "/posts/" + event.postSlug() + "-" + event.postId();

        try {
            EmailMessageDTO emailMessageDTO = emailTemplateService.buildPostLiked(
                    event.authorName(),
                    event.likerName(),
                    event.postTitle(),
                    postUrl
            );

            emailService.sendEmail(event.authorEmail(), emailMessageDTO.getSubject(), emailMessageDTO.getBody());
        } catch (Exception e) {
            log.error("Failed to send post liked notification email to author: {}. Error: {}",
                    event.authorName(), e.getMessage());
        }
    }

    @Async("emailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNewFollowerEvent(NewFollowerEvent event) {

        String profileUrl = baseUrl + "/users/" + event.followerUsername() + "-" + event.followerId();

        try {
            EmailMessageDTO emailMessageDTO = emailTemplateService.buildNewFollower(
                    event.followeeName(),
                    event.followerName(),
                    profileUrl
            );

            emailService.sendEmail(event.followeeEmail(), emailMessageDTO.getSubject(), emailMessageDTO.getBody());
        } catch (Exception e) {
            log.error("Failed to build or send new follower email for followee: {} and follower: {}. Error: {}",
                    event.followeeName(), event.followerName(), e.getMessage());
        }
    }
}
