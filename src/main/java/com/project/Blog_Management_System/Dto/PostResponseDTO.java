package com.project.Blog_Management_System.Dto;

import lombok.Data;

@Data
public class PostResponseDTO {
    private String id;
    private String slug;
    private String title;
    private String description;
    private String content;
    private Integer likeCount;
    private Integer commentCount;
    private UserInfoDTO user;
    private CategoryResponseDTO category;
    private Boolean isOwner;
}
