package com.project.Blog_Management_System.Utils;

import com.project.Blog_Management_System.Entities.CategoryEntity;
import com.project.Blog_Management_System.Entities.CommentEntity;
import com.project.Blog_Management_System.Entities.PostEntity;
import com.project.Blog_Management_System.Entities.UserEntity;
import com.project.Blog_Management_System.Enums.PostStatus;
import com.project.Blog_Management_System.Exceptions.InvalidActionException;
import com.project.Blog_Management_System.Exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidationUtilsTest {

    @Mock
    private MessageService messageService;

    @InjectMocks
    private ValidationUtils validationUtils;

    @Nested
    @DisplayName("isInvalidCategory()")
    class IsInvalidCategory {

        @Test
        @DisplayName("does nothing when category is valid and slug matches")
        void doesNothingWhenCategoryIsValid() {
            CategoryEntity category = new CategoryEntity();
            category.setSlug("tech-trends");

            assertDoesNotThrow(() -> validationUtils.isInvalidCategory(category, "tech-trends"));
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when category is null")
        void throwsExceptionWhenCategoryIsNull() {
            when(messageService.get(eq("exception.resource.not_found"), eq("Category")))
                    .thenReturn("Category not found");

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                    validationUtils.isInvalidCategory(null, "tech-trends")
            );
            assertEquals("Category not found", exception.getMessage());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when category slug does not match")
        void throwsExceptionWhenSlugMismatches() {
            CategoryEntity category = new CategoryEntity();
            category.setSlug("lifestyle");
            when(messageService.get(eq("exception.resource.not_found"), eq("Category")))
                    .thenReturn("Category not found");

            assertThrows(ResourceNotFoundException.class, () ->
                    validationUtils.isInvalidCategory(category, "tech-trends")
            );
        }
    }

    @Nested
    @DisplayName("isInvalidUser()")
    class IsInvalidUser {

        @Test
        @DisplayName("does nothing when user is valid and username matches case-insensitively")
        void doesNothingWhenUserIsValid() {
            UserEntity user = new UserEntity();
            user.setUsername("john_doe");
            user.setIsDeleted(false);

            assertDoesNotThrow(() -> validationUtils.isInvalidUser(user, "JOHN_DOE"));
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when user is null")
        void throwsExceptionWhenUserIsNull() {
            when(messageService.get(eq("exception.resource.not_found"), eq("User account")))
                    .thenReturn("User account not found");

            assertThrows(ResourceNotFoundException.class, () ->
                    validationUtils.isInvalidUser(null, "john_doe")
            );
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when user is marked as deleted")
        void throwsExceptionWhenUserIsDeleted() {
            UserEntity user = new UserEntity();
            user.setUsername("john_doe");
            user.setIsDeleted(true);
            when(messageService.get(eq("exception.resource.not_found"), eq("User account")))
                    .thenReturn("User account not found");

            assertThrows(ResourceNotFoundException.class, () ->
                    validationUtils.isInvalidUser(user, "john_doe")
            );
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when username does not match")
        void throwsExceptionWhenUsernameMismatches() {
            UserEntity user = new UserEntity();
            user.setUsername("john_doe");
            user.setIsDeleted(false);
            when(messageService.get(eq("exception.resource.not_found"), eq("User account")))
                    .thenReturn("User account not found");

            assertThrows(ResourceNotFoundException.class, () ->
                    validationUtils.isInvalidUser(user, "jane_doe")
            );
        }
    }

    @Nested
    @DisplayName("isInvalidPost()")
    class IsInvalidPost {

        @Test
        @DisplayName("does nothing when post is valid and slug matches")
        void doesNothingWhenPostIsValid() {
            PostEntity post = new PostEntity();
            post.setSlug("my-first-blog");

            assertDoesNotThrow(() -> validationUtils.isInvalidPost(post, "my-first-blog"));
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when post is null")
        void throwsExceptionWhenPostIsNull() {
            when(messageService.get(eq("exception.resource.not_found"), eq("Post")))
                    .thenReturn("Post not found");

            assertThrows(ResourceNotFoundException.class, () ->
                    validationUtils.isInvalidPost(null, "my-first-blog")
            );
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when post slug does not match")
        void throwsExceptionWhenPostSlugMismatches() {
            PostEntity post = new PostEntity();
            post.setSlug("another-blog");
            when(messageService.get(eq("exception.resource.not_found"), eq("Post")))
                    .thenReturn("Post not found");

            assertThrows(ResourceNotFoundException.class, () ->
                    validationUtils.isInvalidPost(post, "my-first-blog")
            );
        }
    }

    @Nested
    @DisplayName("isPublishedPost()")
    class IsPublishedPost {

        @Test
        @DisplayName("does nothing when post status is PUBLISHED")
        void doesNothingWhenPostIsPublished() {
            PostEntity post = new PostEntity();
            post.setStatus(PostStatus.PUBLISHED);
            UserEntity reader = new UserEntity();

            assertDoesNotThrow(() -> validationUtils.isPublishedPost(post, reader));
        }

        @Test
        @DisplayName("throws InvalidActionException when post is unpublished but belongs to the active user")
        void throwsInvalidActionForOwnerOfUnpublishedPost() {
            UserEntity author = new UserEntity();
            PostEntity post = new PostEntity();
            post.setStatus(PostStatus.DRAFT);
            post.setUser(author);

            when(messageService.get("exception.invalid.action.unpublished_post"))
                    .thenReturn("Cannot view unpublished post");

            InvalidActionException exception = assertThrows(InvalidActionException.class, () ->
                    validationUtils.isPublishedPost(post, author)
            );
            assertEquals("Cannot view unpublished post", exception.getMessage());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when post is unpublished and belongs to a different user")
        void throwsResourceNotFoundForNonOwnerOfUnpublishedPost() {
            UserEntity author = new UserEntity();
            author.setUsername("author_user");
            author.setId(UUID.randomUUID());

            UserEntity reader = new UserEntity();
            reader.setUsername("different_reader_user");
            reader.setId(UUID.randomUUID());

            PostEntity post = new PostEntity();
            post.setStatus(PostStatus.DRAFT);
            post.setUser(author);

            when(messageService.get(eq("exception.resource.not_found"), eq("Post")))
                    .thenReturn("Post not found");

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                    validationUtils.isPublishedPost(post, reader)
            );
            assertEquals("Post not found", exception.getMessage());
        }
    }

        @Nested
    @DisplayName("isInvalidComment()")
    class IsInvalidComment {

        @Test
        @DisplayName("does nothing when comment is not null")
        void doesNothingWhenCommentIsNotNull() {
            CommentEntity comment = new CommentEntity();
            assertDoesNotThrow(() -> validationUtils.isInvalidComment(comment));
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when comment is null")
        void throwsExceptionWhenCommentIsNull() {
            when(messageService.get(eq("exception.resource.not_found"), eq("Comment")))
                    .thenReturn("Comment not found");

            assertThrows(ResourceNotFoundException.class, () ->
                    validationUtils.isInvalidComment(null)
            );
        }
    }

    @Nested
    @DisplayName("validateReplyDepth()")
    class ValidateReplyDepth {

        @Test
        @DisplayName("does nothing when parent comment depth is exactly 0")
        void doesNothingWhenDepthIsZero() {
            CommentEntity comment = new CommentEntity();
            comment.setDepth(0);

            assertDoesNotThrow(() -> validationUtils.validateReplyDepth(comment));
        }

        @Test
        @DisplayName("throws IllegalArgumentException when parent comment depth is 1 or more")
        void throwsExceptionWhenDepthIsTooDeep() {
            CommentEntity comment = new CommentEntity();
            comment.setDepth(1);

            when(messageService.get("exception.illegal.argument.invalid_comment_depth"))
                    .thenReturn("Comment depth exceeded");

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> validationUtils.validateReplyDepth(comment));
            assertEquals("Comment depth exceeded", exception.getMessage());
        }
    }
}