package com.project.Blog_Management_System.Dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PostRequestDTO {
    @NotNull
    @Size(min = 10, max = 100, message = "Title must be between 10 and 100 characters long")
    private String title;

    @NotNull
    @Size(min = 50, max = 500, message = "Description must be between 50 and 500 characters long")
    private String description;

    @NotNull
    @Size(min = 100, max = 125000, message = "Content must be between 100 and 125000 characters long")
    private String content;

    @NotNull
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Category slug must be a valid slug format (lowercase letters, numbers, and hyphens)")
    private String categorySlug;
}
