package com.project.Blog_Management_System.Utils;

import com.project.Blog_Management_System.Entities.CategoryEntity;
import com.project.Blog_Management_System.Entities.CommentEntity;
import com.project.Blog_Management_System.Entities.PostEntity;
import com.project.Blog_Management_System.Entities.UserEntity;
import com.project.Blog_Management_System.Enums.PostStatus;
import com.project.Blog_Management_System.Exceptions.InvalidActionException;
import com.project.Blog_Management_System.Exceptions.ResourceNotFoundException;

public class ValidationUtils {

    /**
     * Validates a {@link CategoryEntity} and throws {@link ResourceNotFoundException} if the category is invalid.
     *
     * @param category The {@link CategoryEntity} to validate.
     * @param slug     The expected {@code slug} to match against the {@link CategoryEntity}.
     * @throws ResourceNotFoundException if the {@code category} is null or the {@code slug} does not match
     */
    public static void isInvalidCategory(CategoryEntity category, String slug) {
        if (category == null || !category.getSlug().equals(slug)) {
            throw new ResourceNotFoundException("Category does not exist");
        }
    }

    /**
     * Validates a {@link UserEntity} and throws {@link ResourceNotFoundException} if the user is invalid.
     *
     * @param user     The {@link UserEntity} to validate.
     * @param username The expected {@code username} to match against the {@link UserEntity}.
     * @throws ResourceNotFoundException if the {@code user} is null, marked as deleted, or the {@code username} does not match.
     */
    public static void isInvalidUser(UserEntity user, String username) {
        if (user == null || user.getIsDeleted() || !user.getUsername().equalsIgnoreCase(username)) {
            throw new ResourceNotFoundException("User account does not exist");
        }
    }

    /**
     * Validates a {@link PostEntity} and throws {@link ResourceNotFoundException} if the post is invalid.
     *
     * @param post The {@link PostEntity} to validate.
     * @param slug The expected {@code slug} to match against the {@link PostEntity}.
     * @throws ResourceNotFoundException if the {@code post} is null or the {@code slug} does not match.
     */
    public static void isInvalidPost(PostEntity post, String slug) {
        if (post == null || !post.getSlug().equals(slug)) {
            throw new ResourceNotFoundException("Post does not exist");
        }
    }

    /**
     * Validates a {@link CommentEntity} and throws {@link ResourceNotFoundException} if the comment is invalid.
     *
     * @param comment The {@link CommentEntity} to validate.
     * @throws ResourceNotFoundException if the {@code comment} is null.
     */
    public static void isInvalidComment(CommentEntity comment) {
        if (comment == null) {
            throw new ResourceNotFoundException("Comment does not exist");
        }
    }

    /**
    * Validates that a {@link PostEntity} is published and throws {@link ResourceNotFoundException} if it is not.
    *
    * @param post The {@link PostEntity} to validate.
    * @throws ResourceNotFoundException if the {@code post} is not in the PUBLISHED status.
    */
    public static void isPublishedPost(PostEntity post, UserEntity user) {
        if (post.getStatus() != PostStatus.PUBLISHED) {
            throw (post.getUser().equals(user)) ?
                    new InvalidActionException("Invalid action on unpublished post")
                    : new ResourceNotFoundException("Post not found");
        }
    }

}
