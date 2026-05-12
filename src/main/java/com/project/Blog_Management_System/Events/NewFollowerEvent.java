package com.project.Blog_Management_System.Events;

import lombok.Builder;

import java.util.UUID;

@Builder
public record NewFollowerEvent(
        String followeeName,
        String followeeEmail,
        String followerName,
        String followerUsername,
        UUID followerId
) {}
