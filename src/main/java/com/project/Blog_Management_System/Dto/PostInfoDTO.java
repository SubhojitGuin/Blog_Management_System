package com.project.Blog_Management_System.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostInfoDTO {
    private UUID id;
    private String slug;
    private String title;
    private String description;
    private Integer readingTimeMinutes;
    private Integer likeCount;
    private Integer commentCount;
}
