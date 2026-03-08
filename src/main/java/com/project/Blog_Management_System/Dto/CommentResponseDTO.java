package com.project.Blog_Management_System.Dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentResponseDTO {
    private String body;
    private UserInfoDTO user;
    private LocalDateTime createdAt;
    private Boolean isAuthor;
}
