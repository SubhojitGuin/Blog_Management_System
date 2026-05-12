package com.project.Blog_Management_System.Events;

import lombok.Builder;

import java.util.UUID;

@Builder
public record PostLikedEvent(
        String authorName,
        String authorEmail,
        String likerName,
        String postTitle,
        String postSlug,
        UUID postId
) {}
