package com.project.Blog_Management_System.Events;

import lombok.Builder;

import java.util.UUID;

@Builder
public record NewPostPublishedEvent(
        UUID postId,
        String postSlug,
        String postTitle,
        UUID authorId,
        String authorName
) {}
