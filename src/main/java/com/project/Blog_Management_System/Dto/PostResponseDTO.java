package com.project.Blog_Management_System.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
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
    private Boolean isOwner = true;
    private Boolean isLiked = false;
}
