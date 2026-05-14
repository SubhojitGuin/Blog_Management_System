package com.project.Blog_Management_System.Utils;

import com.project.Blog_Management_System.Entities.CommentEntity;
import com.project.Blog_Management_System.Entities.PostEntity;
import com.project.Blog_Management_System.Entities.UserEntity;
import com.project.Blog_Management_System.Enums.PostStatus;
import com.project.Blog_Management_System.Enums.Role;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AppUtils {

    private final MessageService messageService;

    /**
     * Retrieves the currently authenticated user from the security context.
     *
     * @return The current user as a {@link UserEntity} object.
     */
    public static UserEntity getCurrentUser() {
        return (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    /**
     * Checks if the currently authenticated user has a specific role.
     *
     * @param role The role to check for.
     * @return {@code true} if the user has the specified role, false otherwise.
     */
    public static boolean hasRole(Role role) {
        return Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getAuthorities().stream().anyMatch(
                authority -> Objects.equals(authority.getAuthority(), "ROLE_" + role.name())
        );
    }

    /**
     * Generates a URL-friendly slug from a given name.
     *
     * @param name The input string to be converted into a slug.
     * @return A slugified version of the input string.
     */
    public static String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "-");
    }

    /**
     * Converts a list of sort field strings into a Sort object, validating against allowed fields.
     *
     * @param sortFields          A list of strings representing the sort fields and directions (e.g., "name:asc").
     * @param ALLOWED_SORT_FIELDS A set of allowed field names for sorting.
     * @return A {@link Sort} object representing the sorting criteria.
     * @throws IllegalArgumentException if any of the sort fields are invalid.
     */
    public Sort convertToSort(List<String> sortFields, Set<String> ALLOWED_SORT_FIELDS) {
        List<Sort.Order> orders = new ArrayList<>();

        for (String field : sortFields) {
            String[] propertyAndDirection = field.split(":");
            String property = propertyAndDirection[0];

            if (!ALLOWED_SORT_FIELDS.contains(property)) {
                throw new IllegalArgumentException(messageService.get("exception.illegal.argument.invalid_sort_field", property));
            }

            Sort.Direction direction = Sort.DEFAULT_DIRECTION;

            if (propertyAndDirection.length > 1) {
                String directionString = propertyAndDirection[1];
                direction = Sort.Direction.fromOptionalString(directionString)
                        .orElse(Sort.DEFAULT_DIRECTION);
            }

            Sort.Order order = new Sort.Order(direction, property);
            orders.add(order);
        }
        return Sort.by(orders);
    }

    /**
     * Applies the given post status and publish date to the provided post entity.
     *
     * @param post      The {@link PostEntity} to update.
     * @param status    The desired {@link PostStatus} to set on the post. If null, defaults to DRAFT.
     * @param publishAt The desired publishing date and time for the post. Required if status is SCHEDULED.
     * @throws IllegalArgumentException if publishAt is null when status is SCHEDULED.
     */
    public void applyStatusAndPublishAt(PostEntity post,
                                         PostStatus status,
                                         LocalDateTime publishAt) {
        if (status == null) {
            status = PostStatus.DRAFT;
        }

        switch (status) {
            case SCHEDULED -> {
                if (publishAt == null) {
                    throw new IllegalArgumentException(messageService.get("exception.illegal.argument.publish_at_required_for_scheduled_status"));
                }

                post.setStatus(PostStatus.SCHEDULED);
                post.setPublishAt(publishAt);
            }
            case PUBLISHED -> {
                post.setStatus(PostStatus.PUBLISHED);
                post.setPublishAt(null);
            }
            default -> {
                post.setStatus(PostStatus.DRAFT);
                post.setPublishAt(null);
            }
        }
    }

    /**
     * Generates a snippet of the comment body, limited to 100 characters.
     *
     * @param comment The {@link CommentEntity} for which to generate the snippet.
     * @return A string containing the first 100 characters of the comment body, followed by "..." if it exceeds 100 characters.
     */
    public static @NonNull String getCommentSnippet(CommentEntity comment) {
        return comment.getBody().length() > 100
                ? comment.getBody().substring(0, 100) + "..."
                : comment.getBody();
    }

}
