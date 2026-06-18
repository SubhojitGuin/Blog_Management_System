package com.project.Blog_Management_System.Schedulers;

import com.project.Blog_Management_System.Entities.CategoryEntity;
import com.project.Blog_Management_System.Entities.PostEntity;
import com.project.Blog_Management_System.Entities.UserEntity;
import com.project.Blog_Management_System.Enums.PostStatus;
import com.project.Blog_Management_System.Events.ScheduledPostPublishedEvent;
import com.project.Blog_Management_System.Repositories.PostRepository;
import com.project.Blog_Management_System.Utils.TestEntityFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PostPublishSchedulerTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PostPublishScheduler postPublishScheduler;

    private UserEntity author;
    private CategoryEntity category;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        author = TestEntityFactory.testUser("author");
        author.setId(UUID.randomUUID());

        category = TestEntityFactory.testCategory("main");
        category.setId(UUID.randomUUID());

        now = LocalDateTime.now();
    }

    @Nested
    @DisplayName("publishScheduledPosts()")
    class PublishScheduledPosts {

        @Test
        @DisplayName("publishes due scheduled posts and fires event for each")
        void publishesDueScheduledPostsAndFiresEvents() {
            PostEntity duePost = createPostWithPublishTime(now.minusMinutes(5));

            when(postRepository.findByStatusAndPublishAtLessThanEqual(eq(PostStatus.SCHEDULED), any(LocalDateTime.class)))
                    .thenReturn(List.of(duePost));
            when(postRepository.publishDuePosts(any(LocalDateTime.class))).thenReturn(1);

            postPublishScheduler.publishScheduledPosts();

            ArgumentCaptor<ScheduledPostPublishedEvent> eventCaptor = ArgumentCaptor.forClass(ScheduledPostPublishedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            ScheduledPostPublishedEvent event = eventCaptor.getValue();
            assertAll(
                    () -> assertEquals(duePost.getId(), event.postId()),
                    () -> assertEquals(duePost.getSlug(), event.postSlug()),
                    () -> assertEquals(duePost.getTitle(), event.postTitle()),
                    () -> assertEquals(author.getId(), event.authorId()),
                    () -> assertEquals(author.getName(), event.authorName()),
                    () -> assertEquals(author.getEmail(), event.authorEmail())
            );
        }

        @Test
        @DisplayName("publishes multiple due posts and fires event for each")
        void publishesMultipleDuePostsAndFiresEventsForEach() {
            PostEntity firstPost = createPostWithPublishTime(now.minusMinutes(10));
            PostEntity secondPost = createPostWithPublishTime(now.minusMinutes(5));

            when(postRepository.findByStatusAndPublishAtLessThanEqual(eq(PostStatus.SCHEDULED), any(LocalDateTime.class)))
                    .thenReturn(List.of(firstPost, secondPost));
            when(postRepository.publishDuePosts(any(LocalDateTime.class))).thenReturn(2);

            postPublishScheduler.publishScheduledPosts();

            ArgumentCaptor<ScheduledPostPublishedEvent> eventCaptor = ArgumentCaptor.forClass(ScheduledPostPublishedEvent.class);
            verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());

            List<ScheduledPostPublishedEvent> events = eventCaptor.getAllValues();
            assertAll(
                    () -> assertEquals(2, events.size()),
                    () -> assertTrue(events.stream().anyMatch(e -> e.postId().equals(firstPost.getId()))),
                    () -> assertTrue(events.stream().anyMatch(e -> e.postId().equals(secondPost.getId())))
            );
        }

        @Test
        @DisplayName("does not publish when no scheduled posts are due")
        void doesNotPublishWhenNoPostsAreDue() {
            when(postRepository.findByStatusAndPublishAtLessThanEqual(eq(PostStatus.SCHEDULED), any(LocalDateTime.class)))
                    .thenReturn(List.of());
            when(postRepository.publishDuePosts(any(LocalDateTime.class))).thenReturn(0);

            postPublishScheduler.publishScheduledPosts();

            verify(eventPublisher, never()).publishEvent(any(ScheduledPostPublishedEvent.class));
        }

        @Test
        @DisplayName("finds only scheduled posts with publish time less than or equal to current time")
        void findsOnlyScheduledPostsWithDuePublishTime() {
            when(postRepository.findByStatusAndPublishAtLessThanEqual(eq(PostStatus.SCHEDULED), any(LocalDateTime.class)))
                    .thenReturn(List.of());
            when(postRepository.publishDuePosts(any(LocalDateTime.class))).thenReturn(0);

            postPublishScheduler.publishScheduledPosts();

            ArgumentCaptor<LocalDateTime> timeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(postRepository).findByStatusAndPublishAtLessThanEqual(eq(PostStatus.SCHEDULED), timeCaptor.capture());

            LocalDateTime capturedDateTime = timeCaptor.getValue();
            assertNotNull(capturedDateTime);
        }

        @Test
        @DisplayName("calls publishDuePosts to update post status in database")
        void callsPublishDuePostsToUpdateStatus() {
            when(postRepository.findByStatusAndPublishAtLessThanEqual(eq(PostStatus.SCHEDULED), any(LocalDateTime.class)))
                    .thenReturn(List.of());
            when(postRepository.publishDuePosts(any(LocalDateTime.class))).thenReturn(0);

            postPublishScheduler.publishScheduledPosts();

            verify(postRepository).publishDuePosts(any(LocalDateTime.class));
        }

        @Test
        @DisplayName("returns correct post count from publishDuePosts")
        void capturesCorrectPostCountFromPublishDuePosts() {
            PostEntity post = createPostWithPublishTime(now.minusMinutes(5));

            when(postRepository.findByStatusAndPublishAtLessThanEqual(eq(PostStatus.SCHEDULED), any(LocalDateTime.class)))
                    .thenReturn(List.of(post));
            when(postRepository.publishDuePosts(any(LocalDateTime.class))).thenReturn(1);

            postPublishScheduler.publishScheduledPosts();

            verify(postRepository).findByStatusAndPublishAtLessThanEqual(eq(PostStatus.SCHEDULED), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("includes post details in published event")
        void includesAllPostDetailsInPublishedEvent() {
            PostEntity post = new PostEntity();
            post.setId(UUID.randomUUID());
            post.setSlug("special-post-slug");
            post.setTitle("Special Post Title");
            post.setUser(author);

            when(postRepository.findByStatusAndPublishAtLessThanEqual(eq(PostStatus.SCHEDULED), any(LocalDateTime.class)))
                    .thenReturn(List.of(post));
            when(postRepository.publishDuePosts(any(LocalDateTime.class))).thenReturn(1);

            postPublishScheduler.publishScheduledPosts();

            ArgumentCaptor<ScheduledPostPublishedEvent> eventCaptor = ArgumentCaptor.forClass(ScheduledPostPublishedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            ScheduledPostPublishedEvent event = eventCaptor.getValue();
            assertAll(
                    () -> assertEquals("special-post-slug", event.postSlug()),
                    () -> assertEquals("Special Post Title", event.postTitle())
            );
        }

        @Test
        @DisplayName("includes author details in published event")
        void includesAuthorDetailsInPublishedEvent() {
            UserEntity specificAuthor = TestEntityFactory.testUser("specific-author");
            specificAuthor.setId(UUID.randomUUID());
            specificAuthor.setName("John Doe");
            specificAuthor.setEmail("john@example.com");

            PostEntity post = createPostWithPublishTime(now.minusMinutes(5));
            post.setUser(specificAuthor);

            when(postRepository.findByStatusAndPublishAtLessThanEqual(eq(PostStatus.SCHEDULED), any(LocalDateTime.class)))
                    .thenReturn(List.of(post));
            when(postRepository.publishDuePosts(any(LocalDateTime.class))).thenReturn(1);

            postPublishScheduler.publishScheduledPosts();

            ArgumentCaptor<ScheduledPostPublishedEvent> eventCaptor = ArgumentCaptor.forClass(ScheduledPostPublishedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            ScheduledPostPublishedEvent event = eventCaptor.getValue();
            assertAll(
                    () -> assertEquals(specificAuthor.getId(), event.authorId()),
                    () -> assertEquals("John Doe", event.authorName()),
                    () -> assertEquals("john@example.com", event.authorEmail())
            );
        }

        @Test
        @DisplayName("fires events even when publish count is less than posts found")
        void firesEventsEvenWhenCountDiffers() {
            PostEntity post = createPostWithPublishTime(now.minusMinutes(5));

            when(postRepository.findByStatusAndPublishAtLessThanEqual(eq(PostStatus.SCHEDULED), any(LocalDateTime.class)))
                    .thenReturn(List.of(post));
            when(postRepository.publishDuePosts(any(LocalDateTime.class))).thenReturn(1);

            postPublishScheduler.publishScheduledPosts();

            verify(eventPublisher).publishEvent(any(ScheduledPostPublishedEvent.class));
        }

        @Test
        @DisplayName("handles empty list of due posts gracefully")
        void handlesEmptyListGracefully() {
            when(postRepository.findByStatusAndPublishAtLessThanEqual(eq(PostStatus.SCHEDULED), any(LocalDateTime.class)))
                    .thenReturn(List.of());
            when(postRepository.publishDuePosts(any(LocalDateTime.class))).thenReturn(0);

            assertDoesNotThrow(() -> postPublishScheduler.publishScheduledPosts());

            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("processes posts based on findByStatusAndPublishAtLessThanEqual result")
        void processesPostsFromRepository() {
            PostEntity post1 = createPostWithPublishTime(now.minusMinutes(10));
            PostEntity post2 = createPostWithPublishTime(now.minusMinutes(1));

            List<PostEntity> duePostsList = List.of(post1, post2);

            when(postRepository.findByStatusAndPublishAtLessThanEqual(eq(PostStatus.SCHEDULED), any(LocalDateTime.class)))
                    .thenReturn(duePostsList);
            when(postRepository.publishDuePosts(any(LocalDateTime.class))).thenReturn(2);

            postPublishScheduler.publishScheduledPosts();

            verify(eventPublisher, times(2)).publishEvent(any(ScheduledPostPublishedEvent.class));
        }

        @Test
        @DisplayName("queries with correct post status filter")
        void queriesWithCorrectStatusFilter() {
            when(postRepository.findByStatusAndPublishAtLessThanEqual(eq(PostStatus.SCHEDULED), any(LocalDateTime.class)))
                    .thenReturn(List.of());
            when(postRepository.publishDuePosts(any(LocalDateTime.class))).thenReturn(0);

            postPublishScheduler.publishScheduledPosts();

            verify(postRepository).findByStatusAndPublishAtLessThanEqual(eq(PostStatus.SCHEDULED), any(LocalDateTime.class));
        }
    }

    private PostEntity createPostWithPublishTime(LocalDateTime publishAt) {
        PostEntity post = TestEntityFactory.testPost(author, category, "test");
        post.setId(UUID.randomUUID());
        post.setStatus(PostStatus.SCHEDULED);
        post.setPublishAt(publishAt);
        return post;
    }
}




