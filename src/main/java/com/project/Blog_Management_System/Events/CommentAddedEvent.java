package com.project.Blog_Management_System.Events;

import lombok.Builder;

import java.util.UUID;

@Builder
public record CommentAddedEvent(
        String authorName,
        String authorEmail,
        String commenterName,
        String postTitle,
        String postSlug,
        UUID postId,
        String commentSnippet
) {}
