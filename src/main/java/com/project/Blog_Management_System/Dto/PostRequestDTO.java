package com.project.Blog_Management_System.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PostRequestDTO {
    @NotBlank
    @Size(min = 2, max = 100)
    private String title;

    @NotBlank
    @Size(min = 50, max = 2000)
    private String description;

    @NotBlank
    @Size(min = 100, max = 125000)
    private String content;

    @NotBlank
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug must be lowercase alphanumeric with hyphens only")
    @Size(min = 2, max = 100)
    private String categorySlug;
}
