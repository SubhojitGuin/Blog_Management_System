package com.project.Blog_Management_System.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentResponseDTO {
    private UUID id;
    private String body;
    private UserInfoDTO user;
    private LocalDateTime createdAt;
    private Boolean isAuthor = true;
}
