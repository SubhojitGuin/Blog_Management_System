package com.project.Blog_Management_System.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentRequestDTO {
    @NotBlank
    @Size(min = 2, max = 1000)
    private String body;
}
