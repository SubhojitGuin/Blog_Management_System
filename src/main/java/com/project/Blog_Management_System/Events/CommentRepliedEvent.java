package com.project.Blog_Management_System.Events;

import lombok.Builder;

import java.util.UUID;

@Builder
public record CommentRepliedEvent(
        String parentCommenterName,
        String parentCommenterEmail,
        String childCommenterName,
        String postTitle,
        String postSlug,
        UUID postId,
        String originalCommentSnippet,
        String replyCommentSnippet
) {}
