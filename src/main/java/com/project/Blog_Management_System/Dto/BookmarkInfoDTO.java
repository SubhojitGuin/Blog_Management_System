package com.project.Blog_Management_System.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class BookmarkInfoDTO {
    private UUID id;
    private PostInfoDTO post;
    private LocalDateTime bookmarkedAt;
}
