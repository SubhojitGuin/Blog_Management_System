package com.project.Blog_Management_System.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentResponseDTO {
    private String body;
    private UserInfoDTO user;
    private LocalDateTime createdAt;
    private Boolean isAuthor = true;
}
