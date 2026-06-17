package com.project.Blog_Management_System.Listeners;

import com.project.Blog_Management_System.Dto.EmailMessageDTO;
import com.project.Blog_Management_System.Entities.FollowEntity;
import com.project.Blog_Management_System.Entities.UserEntity;
import com.project.Blog_Management_System.Events.*;
import com.project.Blog_Management_System.Repositories.FollowRepository;
import com.project.Blog_Management_System.Service.Interfaces.EmailService;
import com.project.Blog_Management_System.Service.Interfaces.EmailTemplateService;
import com.project.Blog_Management_System.Utils.TestEntityFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailEventListenersTest {

    @Mock
    private EmailService emailService;

    @Mock
    private FollowRepository followRepository;

    @Mock
    private EmailTemplateService emailTemplateService;

    @InjectMocks
    private EmailEventListeners emailEventListeners;

    private void setBaseUrl(String baseUrl) {
        ReflectionTestUtils.setField(emailEventListeners, "baseUrl", baseUrl);
    }

    @Nested
    @DisplayName("handleNewPostPublished(NewPostPublishedEvent)")
    class HandleNewPostPublished {

        @Test
        @DisplayName("sends new post email to all followers when post is published")
        void sendsNewPostEmailToAllFollowersWhenPostIsPublished() throws Exception {
            setBaseUrl("http://localhost:8080");
            UUID authorId = UUID.randomUUID();
            UUID postId = UUID.randomUUID();

            UserEntity follower1 = TestEntityFactory.testUser("follower1");
            UserEntity follower2 = TestEntityFactory.testUser("follower2");

            FollowEntity follow1 = FollowEntity.builder().follower(follower1).following(TestEntityFactory.testUser("author")).build();
            FollowEntity follow2 = FollowEntity.builder().follower(follower2).following(TestEntityFactory.testUser("author")).build();

            NewPostPublishedEvent event = NewPostPublishedEvent.builder()
                    .postId(postId)
                    .postSlug("new-post-slug")
                    .postTitle("New Post Title")
                    .authorId(authorId)
                    .authorName("John Author")
                    .build();

            EmailMessageDTO emailMessage = new EmailMessageDTO();
            emailMessage.setSubject("New Post Subject");
            emailMessage.setBody("New Post Body");

            when(followRepository.findByFollowingId(authorId)).thenReturn(List.of(follow1, follow2));
            when(emailTemplateService.buildNewPostByAuthor(anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(emailMessage);

            emailEventListeners.handleNewPostPublished(event);

            ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
            verify(emailService, times(2)).sendEmail(emailCaptor.capture(), anyString(), anyString());
            List<String> sentEmails = emailCaptor.getAllValues();
            assertEquals(follower1.getEmail(), sentEmails.get(0));
            assertEquals(follower2.getEmail(), sentEmails.get(1));
        }

        @Test
        @DisplayName("sends no emails when author has no followers")
        void sendsNoEmailsWhenAuthorHasNoFollowers() throws Exception {
            setBaseUrl("http://localhost:8080");
            UUID authorId = UUID.randomUUID();

            NewPostPublishedEvent event = NewPostPublishedEvent.builder()
                    .postId(UUID.randomUUID())
                    .postSlug("new-post")
                    .postTitle("New Post")
                    .authorId(authorId)
                    .authorName("John Author")
                    .build();

            when(followRepository.findByFollowingId(authorId)).thenReturn(List.of());

            emailEventListeners.handleNewPostPublished(event);

            verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
            verify(emailTemplateService, never()).buildNewPostByAuthor(anyString(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("continues processing when email sending fails for a follower")
        void continuesProcessingWhenEmailSendingFailsForAFollower() throws Exception {
            setBaseUrl("http://localhost:8080");
            UUID authorId = UUID.randomUUID();

            UserEntity follower1 = TestEntityFactory.testUser("follower1");
            UserEntity follower2 = TestEntityFactory.testUser("follower2");

            FollowEntity follow1 = FollowEntity.builder().follower(follower1).build();
            FollowEntity follow2 = FollowEntity.builder().follower(follower2).build();

            NewPostPublishedEvent event = NewPostPublishedEvent.builder()
                    .postId(UUID.randomUUID())
                    .postSlug("new-post")
                    .postTitle("New Post")
                    .authorId(authorId)
                    .authorName("John Author")
                    .build();

            EmailMessageDTO emailMessage = new EmailMessageDTO();
            emailMessage.setSubject("New Post Subject");
            emailMessage.setBody("New Post Body");

            when(followRepository.findByFollowingId(authorId)).thenReturn(List.of(follow1, follow2));
            when(emailTemplateService.buildNewPostByAuthor(anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(emailMessage);
            doThrow(new RuntimeException("Email service error")).when(emailService)
                    .sendEmail(follower1.getEmail(), emailMessage.getSubject(), emailMessage.getBody());

            emailEventListeners.handleNewPostPublished(event);

            verify(emailService, times(2)).sendEmail(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("logs error and continues when template building fails for a follower")
        void logsErrorAndContinuesWhenTemplateBuildingFailsForAFollower() throws Exception {
            setBaseUrl("http://localhost:8080");
            UUID authorId = UUID.randomUUID();

            UserEntity follower1 = TestEntityFactory.testUser("follower1");
            UserEntity follower2 = TestEntityFactory.testUser("follower2");
            FollowEntity follow1 = FollowEntity.builder().follower(follower1).build();
            FollowEntity follow2 = FollowEntity.builder().follower(follower2).build();

            NewPostPublishedEvent event = NewPostPublishedEvent.builder()
                    .postId(UUID.randomUUID())
                    .authorId(authorId)
                    .build();

            EmailMessageDTO emailMessage = new EmailMessageDTO();
            emailMessage.setSubject("Subject");
            emailMessage.setBody("Body");

            when(followRepository.findByFollowingId(authorId)).thenReturn(List.of(follow1, follow2));
            when(emailTemplateService.buildNewPostByAuthor(eq(follower1.getName()), any(), any(), any()))
                    .thenThrow(new RuntimeException("Template layout error"));
            when(emailTemplateService.buildNewPostByAuthor(eq(follower2.getName()), any(), any(), any()))
                    .thenReturn(emailMessage);

            emailEventListeners.handleNewPostPublished(event);

            verify(emailService, times(1)).sendEmail(eq(follower2.getEmail()), anyString(), anyString());
            verify(emailService, never()).sendEmail(eq(follower1.getEmail()), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("handleScheduledPostPublishedEvent(ScheduledPostPublishedEvent)")
    class HandleScheduledPostPublishedEvent {

        @Test
        @DisplayName("sends scheduled post published email to author and all followers")
        void sendsScheduledPostEmailToAuthorAndAllFollowers() throws Exception {
            setBaseUrl("http://localhost:8080");
            UUID authorId = UUID.randomUUID();

            UserEntity follower = TestEntityFactory.testUser("follower");
            FollowEntity follow = FollowEntity.builder().follower(follower).build();

            ScheduledPostPublishedEvent event = ScheduledPostPublishedEvent.builder()
                    .postId(UUID.randomUUID())
                    .postSlug("scheduled-post")
                    .postTitle("Scheduled Post Title")
                    .authorId(authorId)
                    .authorName("Jane Author")
                    .authorEmail("jane@example.com")
                    .build();

            EmailMessageDTO authorMessage = new EmailMessageDTO();
            authorMessage.setSubject("Your Post Published");
            authorMessage.setBody("Published Body");

            EmailMessageDTO followerMessage = new EmailMessageDTO();
            followerMessage.setSubject("New Post Subject");
            followerMessage.setBody("New Post Body");

            when(emailTemplateService.buildPostPublished(anyString(), anyString(), anyString()))
                    .thenReturn(authorMessage);
            when(emailTemplateService.buildNewPostByAuthor(anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(followerMessage);
            when(followRepository.findByFollowingId(authorId)).thenReturn(List.of(follow));

            emailEventListeners.handleScheduledPostPublishedEvent(event);

            verify(emailService).sendEmail("jane@example.com", authorMessage.getSubject(), authorMessage.getBody());
            verify(emailService).sendEmail(follower.getEmail(), followerMessage.getSubject(), followerMessage.getBody());
        }

        @Test
        @DisplayName("sends email to author even if no followers exist")
        void sendsEmailToAuthorEvenIfNoFollowersExist() throws Exception {
            setBaseUrl("http://localhost:8080");
            UUID authorId = UUID.randomUUID();
            ScheduledPostPublishedEvent event = ScheduledPostPublishedEvent.builder().postId(UUID.randomUUID()).postSlug("solo-post").postTitle("Solo Post").authorId(authorId).authorName("Lonely Author").authorEmail("lonely@example.com").build();
            EmailMessageDTO authorMessage = new EmailMessageDTO();
            authorMessage.setSubject("Your Post Published");
            authorMessage.setBody("Body");
            when(emailTemplateService.buildPostPublished(anyString(), anyString(), anyString())).thenReturn(authorMessage);
            when(followRepository.findByFollowingId(authorId)).thenReturn(List.of());
            emailEventListeners.handleScheduledPostPublishedEvent(event);
            verify(emailService).sendEmail("lonely@example.com", authorMessage.getSubject(), authorMessage.getBody());
        }

        @Test
        @DisplayName("continues sending emails to followers if author email fails")
        void continuesSendingEmailsToFollowersIfAuthorEmailFails() throws Exception {
            setBaseUrl("http://localhost:8080");
            UUID authorId = UUID.randomUUID();
            UserEntity follower = TestEntityFactory.testUser("follower");
            FollowEntity follow = FollowEntity.builder().follower(follower).build();
            ScheduledPostPublishedEvent event = ScheduledPostPublishedEvent.builder().postId(UUID.randomUUID()).postSlug("post").postTitle("Post").authorId(authorId).authorName("Author").authorEmail("author@example.com").build();
            EmailMessageDTO authorMessage = new EmailMessageDTO();
            authorMessage.setSubject("Author Subject");
            authorMessage.setBody("Author Body");
            EmailMessageDTO followerMessage = new EmailMessageDTO();
            followerMessage.setSubject("Follower Subject");
            followerMessage.setBody("Follower Body");
            when(emailTemplateService.buildPostPublished(anyString(), anyString(), anyString())).thenReturn(authorMessage);
            when(emailTemplateService.buildNewPostByAuthor(anyString(), anyString(), anyString(), anyString())).thenReturn(followerMessage);
            when(followRepository.findByFollowingId(authorId)).thenReturn(List.of(follow));
            doThrow(new RuntimeException("Author email failed")).when(emailService).sendEmail("author@example.com", authorMessage.getSubject(), authorMessage.getBody());
            emailEventListeners.handleScheduledPostPublishedEvent(event);
            verify(emailService).sendEmail(follower.getEmail(), followerMessage.getSubject(), followerMessage.getBody());
        }

        @Test
        @DisplayName("logs error and continues when follower notification completely template crashes")
        void logsErrorAndContinuesWhenFollowerNotificationTemplateCrashes() throws Exception {
            setBaseUrl("http://localhost:8080");
            UUID authorId = UUID.randomUUID();
            ScheduledPostPublishedEvent event = ScheduledPostPublishedEvent.builder().authorId(authorId).authorEmail("author@example.com").build();
            UserEntity follower = TestEntityFactory.testUser("follower");
            FollowEntity follow = FollowEntity.builder().follower(follower).build();
            when(emailTemplateService.buildPostPublished(any(), any(), any())).thenReturn(new EmailMessageDTO());
            when(followRepository.findByFollowingId(authorId)).thenReturn(List.of(follow));
            when(emailTemplateService.buildNewPostByAuthor(any(), any(), any(), any())).thenThrow(new RuntimeException("Follower template render failure"));
            emailEventListeners.handleScheduledPostPublishedEvent(event);
            verify(emailService, times(1)).sendEmail(eq("author@example.com"), any(), any());
            verify(emailService, never()).sendEmail(eq(follower.getEmail()), any(), any());
        }
    }

    @Nested
    @DisplayName("handleCommentAddedEvent(CommentAddedEvent)")
    class HandleCommentAddedEvent {
        @Test
        @DisplayName("sends comment notification email to post author")
        void sendsCommentNotificationEmailToPostAuthor() throws Exception {
            setBaseUrl("http://localhost:8080");
            CommentAddedEvent event = CommentAddedEvent.builder().authorName("John Post Author").authorEmail("john@example.com").commenterName("Jane Commenter").postTitle("Great Post").postSlug("great-post").postId(UUID.randomUUID()).commentSnippet("Great post!").build();
            EmailMessageDTO emailMessage = new EmailMessageDTO();
            emailMessage.setSubject("New Comment");
            emailMessage.setBody("New Comment Body");
            when(emailTemplateService.buildPostCommented(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(emailMessage);
            emailEventListeners.handleCommentAddedEvent(event);
            verify(emailService).sendEmail("john@example.com", "New Comment", "New Comment Body");
            verify(emailTemplateService).buildPostCommented(eq("John Post Author"), eq("Jane Commenter"), eq("Great Post"), anyString(), eq("Great post!"));
        }

        @Test
        @DisplayName("logs error and continues when email sending fails")
        void logsErrorAndContinuesWhenEmailSendingFails() throws Exception {
            setBaseUrl("http://localhost:8080");
            CommentAddedEvent event = CommentAddedEvent.builder().authorName("John Author").authorEmail("john@example.com").commenterName("Jane Commenter").postTitle("Post").postSlug("post").postId(UUID.randomUUID()).commentSnippet("Comment").build();
            EmailMessageDTO emailMessage = new EmailMessageDTO();
            emailMessage.setSubject("Subject");
            emailMessage.setBody("Body");
            when(emailTemplateService.buildPostCommented(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(emailMessage);
            doThrow(new RuntimeException("Email service down")).when(emailService).sendEmail("john@example.com", "Subject", "Body");
            emailEventListeners.handleCommentAddedEvent(event);
            verify(emailService).sendEmail(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("logs error and suppresses exception when template build throws an error")
        void logsErrorAndSuppressesExceptionWhenTemplateBuildThrows() throws Exception {
            setBaseUrl("http://localhost:8080");
            CommentAddedEvent event = CommentAddedEvent.builder().authorEmail("john@example.com").build();
            when(emailTemplateService.buildPostCommented(any(), any(), any(), any(), any())).thenThrow(new RuntimeException("Template mapping exception"));
            emailEventListeners.handleCommentAddedEvent(event);
            verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("handleCommentRepliedEvent(CommentRepliedEvent)")
    class HandleCommentRepliedEvent {
        @Test
        @DisplayName("sends reply notification email to parent commenter")
        void sendsReplyNotificationEmailToParentCommenter() throws Exception {
            setBaseUrl("http://localhost:8080");
            CommentRepliedEvent event = CommentRepliedEvent.builder().parentCommenterName("Alice").parentCommenterEmail("alice@example.com").childCommenterName("Bob").authorName("Charlie").postTitle("Discussion Post").postSlug("discussion-post").postId(UUID.randomUUID()).originalCommentSnippet("Original comment").replyCommentSnippet("Reply comment").build();
            EmailMessageDTO emailMessage = new EmailMessageDTO();
            emailMessage.setSubject("Reply to Your Comment");
            emailMessage.setBody("Reply Body");
            when(emailTemplateService.buildCommentReplied(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(emailMessage);
            emailEventListeners.handleCommentRepliedEvent(event);
            verify(emailService).sendEmail("alice@example.com", "Reply to Your Comment", "Reply Body");
        }

        @Test
        @DisplayName("constructs post url correctly when handling comment reply")
        void constructsPostUrlCorrectlyWhenHandlingCommentReply() throws Exception {
            setBaseUrl("https://blog.example.com");
            UUID postId = UUID.randomUUID();
            CommentRepliedEvent event = CommentRepliedEvent.builder().parentCommenterName("Alice").parentCommenterEmail("alice@example.com").childCommenterName("Bob").authorName("Charlie").postTitle("Post").postSlug("post-slug").postId(postId).originalCommentSnippet("Original").replyCommentSnippet("Reply").build();
            EmailMessageDTO emailMessage = new EmailMessageDTO();
            emailMessage.setSubject("Subject");
            emailMessage.setBody("Body");
            when(emailTemplateService.buildCommentReplied(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(emailMessage);
            emailEventListeners.handleCommentRepliedEvent(event);
            ArgumentCaptor urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(emailTemplateService).buildCommentReplied(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), (String) urlCaptor.capture());
            assertEquals("https://blog.example.com/posts/post-slug-" + postId, urlCaptor.getValue());
        }

        @Test
        @DisplayName("logs error and swallows failure when exception happens during comment reply processing")
        void logsErrorAndSwallowsFailureWhenExceptionHappens() throws Exception {
            setBaseUrl("https://blog.example.com");
            CommentRepliedEvent event = CommentRepliedEvent.builder().parentCommenterEmail("alice@example.com").build();
            when(emailTemplateService.buildCommentReplied(any(), any(), any(), any(), any(), any(), any())).thenThrow(new RuntimeException("Mail structure compilation crash"));
            emailEventListeners.handleCommentRepliedEvent(event);
            verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("handlePostLikedEvent(PostLikedEvent)")
    class HandlePostLikedEvent {
        @Test
        @DisplayName("sends like notification email to post author")
        void sendsLikeNotificationEmailToPostAuthor() throws Exception {
            setBaseUrl("http://localhost:8080");
            PostLikedEvent event = PostLikedEvent.builder().authorName("Alice Author").authorEmail("alice@example.com").likerName("Bob Liker").postTitle("Awesome Post").postSlug("awesome-post").postId(UUID.randomUUID()).build();
            EmailMessageDTO emailMessage = new EmailMessageDTO();
            emailMessage.setSubject("Your Post Was Liked");
            emailMessage.setBody("Liked Body");
            when(emailTemplateService.buildPostLiked(anyString(), anyString(), anyString(), anyString())).thenReturn(emailMessage);
            emailEventListeners.handlePostLikedEvent(event);
            verify(emailService).sendEmail("alice@example.com", "Your Post Was Liked", "Liked Body");
        }

        @Test
        @DisplayName("logs error and handles exception cleanly when post liked email template building fails")
        void logsErrorAndHandlesExceptionCleanlyWhenTemplateFails() throws Exception {
            setBaseUrl("http://localhost:8080");
            PostLikedEvent event = PostLikedEvent.builder().authorEmail("alice@example.com").build();
            when(emailTemplateService.buildPostLiked(any(), any(), any(), any())).thenThrow(new RuntimeException("Template processing failure"));
            emailEventListeners.handlePostLikedEvent(event);
            verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("handleNewFollowerEvent(NewFollowerEvent)")
    class HandleNewFollowerEvent {
        @Test
        @DisplayName("sends new follower notification email to followee")
        void sendsNewFollowerNotificationEmailToFollowee() throws Exception {
            setBaseUrl("http://localhost:8080");
            UUID followerId = UUID.randomUUID();
            NewFollowerEvent event = NewFollowerEvent.builder().followeeName("Alice").followeeEmail("alice@example.com").followerName("Bob").followerUsername("bob-user").followerId(followerId).build();
            EmailMessageDTO emailMessage = new EmailMessageDTO();
            emailMessage.setSubject("New Follower");
            emailMessage.setBody("New Follower Body");
            when(emailTemplateService.buildNewFollower(anyString(), anyString(), anyString())).thenReturn(emailMessage);
            emailEventListeners.handleNewFollowerEvent(event);
            verify(emailService).sendEmail("alice@example.com", "New Follower", "New Follower Body");
        }

        @Test
        @DisplayName("constructs profile url correctly when handling new follower")
        void constructsProfileUrlCorrectlyWhenHandlingNewFollower() throws Exception {
            setBaseUrl("https://blog.example.com");
            UUID followerId = UUID.randomUUID();
            NewFollowerEvent event = NewFollowerEvent.builder().followeeName("Alice").followeeEmail("alice@example.com").followerName("Bob").followerUsername("bob-username").followerId(followerId).build();
            EmailMessageDTO emailMessage = new EmailMessageDTO();
            emailMessage.setSubject("Subject");
            emailMessage.setBody("Body");
            when(emailTemplateService.buildNewFollower(anyString(), anyString(), anyString())).thenReturn(emailMessage);
            emailEventListeners.handleNewFollowerEvent(event);
            ArgumentCaptor urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(emailTemplateService).buildNewFollower(anyString(), anyString(), (String) urlCaptor.capture());
            assertEquals("https://blog.example.com/users/bob-username-" + followerId, urlCaptor.getValue());
        }

        @Test
        @DisplayName("logs error and handles exception cleanly when handling new follower template execution fails")
        void logsErrorAndHandlesExceptionWhenFollowerProcessingFails() throws Exception {
            setBaseUrl("https://blog.example.com");
            NewFollowerEvent event = NewFollowerEvent.builder().followeeEmail("alice@example.com").build();
            when(emailTemplateService.buildNewFollower(any(), any(), any())).thenThrow(new RuntimeException("Follower profile compilation failure"));
            emailEventListeners.handleNewFollowerEvent(event);
            verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
        }
    }
}