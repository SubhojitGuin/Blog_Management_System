package com.project.Blog_Management_System.Dto;

import lombok.Data;

import java.util.UUID;

@Data
public class PostDTO {
    private String title;
    private String description;
    private String content;
    private UUID categoryId;
}
