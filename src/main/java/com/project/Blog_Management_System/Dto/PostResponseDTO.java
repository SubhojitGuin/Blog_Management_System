package com.project.Blog_Management_System.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class PostResponseDTO {
    private UUID id;
    private String slug;
    private String title;
    private String description;
    private String content;
    private Integer likeCount;
    private Integer commentCount;
    private UserInfoDTO user;
    private CategoryResponseDTO category;
    private Boolean isOwner;
    private Boolean isLiked;
}
