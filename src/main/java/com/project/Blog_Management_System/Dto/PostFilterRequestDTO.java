package com.project.Blog_Management_System.Dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PostFilterRequestDTO {
    @NotBlank
    private String categorySlug;

    @NotBlank
    private String title;
}
