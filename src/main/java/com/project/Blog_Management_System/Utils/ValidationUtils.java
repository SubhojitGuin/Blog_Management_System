package com.project.Blog_Management_System.Utils;

import com.project.Blog_Management_System.Entities.CategoryEntity;
import com.project.Blog_Management_System.Entities.CommentEntity;
import com.project.Blog_Management_System.Entities.PostEntity;
import com.project.Blog_Management_System.Entities.UserEntity;
import com.project.Blog_Management_System.Enums.PostStatus;
import com.project.Blog_Management_System.Exceptions.InvalidActionException;
import com.project.Blog_Management_System.Exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ValidationUtils {

    private final MessageService messageService;

    /**
     * Validates a {@link CategoryEntity} and throws {@link ResourceNotFoundException} if the category is invalid.
     *
     * @param category The {@link CategoryEntity} to validate.
     * @param slug     The expected {@code slug} to match against the {@link CategoryEntity}.
     * @throws ResourceNotFoundException if the {@code category} is null or the {@code slug} does not match
     */
    public void isInvalidCategory(CategoryEntity category, String slug) {
        if (category == null || !category.getSlug().equals(slug)) {
            throw new ResourceNotFoundException(messageService.get("exception.resource.not_found", "Category"));
        }
    }

    /**
     * Validates a {@link UserEntity} and throws {@link ResourceNotFoundException} if the user is invalid.
     *
     * @param user     The {@link UserEntity} to validate.
     * @param username The expected {@code username} to match against the {@link UserEntity}.
     * @throws ResourceNotFoundException if the {@code user} is null, marked as deleted, or the {@code username} does not match.
     */
    public void isInvalidUser(UserEntity user, String username) {
        if (user == null || user.getIsDeleted() || !user.getUsername().equalsIgnoreCase(username)) {
            throw new ResourceNotFoundException(messageService.get("exception.resource.not_found", "User account"));
        }
    }

    /**
     * Validates a {@link PostEntity} and throws {@link ResourceNotFoundException} if the post is invalid.
     *
     * @param post The {@link PostEntity} to validate.
     * @param slug The expected {@code slug} to match against the {@link PostEntity}.
     * @throws ResourceNotFoundException if the {@code post} is null or the {@code slug} does not match.
     */
    public void isInvalidPost(PostEntity post, String slug) {
        if (post == null || !post.getSlug().equals(slug)) {
            throw new ResourceNotFoundException(messageService.get("exception.resource.not_found", "Post"));
        }
    }

    /**
    * Validates that a {@link PostEntity} is published and throws {@link ResourceNotFoundException} if it is not.
    *
    * @param post The {@link PostEntity} to validate.
    * @throws ResourceNotFoundException if the {@code post} is not in the PUBLISHED status.
    */
    public void isPublishedPost(PostEntity post, UserEntity user) {
        if (post.getStatus() != PostStatus.PUBLISHED) {
            throw (post.getUser().equals(user))
                    ? new InvalidActionException(messageService.get("exception.invalid.action.unpublished_post"))
                    : new ResourceNotFoundException(messageService.get("exception.resource.not_found", "Post"));
        }
    }

    /**
     * Validates a {@link CommentEntity} and throws {@link ResourceNotFoundException} if the comment is invalid.
     *
     * @param comment The {@link CommentEntity} to validate.
     * @throws ResourceNotFoundException if the {@code comment} is null.
     */
    public void isInvalidComment(CommentEntity comment) {
        if (comment == null) {
            throw new ResourceNotFoundException(messageService.get("exception.resource.not_found", "Comment"));
        }
    }

    /**
     * Validates that a reply to a comment does not exceed the maximum allowed depth and throws {@link IllegalArgumentException} if it does.
     *
     * @param parentComment The parent {@link CommentEntity} to which the reply is being made.
     * @throws IllegalArgumentException if the depth of the parent comment is greater than or equal to 1.
     */
    public void validateReplyDepth(CommentEntity parentComment) {
        if (parentComment.getDepth() >= 1) {
            throw new IllegalArgumentException(messageService.get("exception.illegal.argument.invalid_comment_depth"));
        }
    }
}
