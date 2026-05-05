package com.project.Blog_Management_System.Dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PostFilterRequestDTO {
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug must be lowercase alphanumeric with hyphens only")
    @Size(min = 2, max = 100)
    private String categorySlug;

    @Size(min = 1, max = 100)
    private String title;

    @PositiveOrZero
    private Integer maxReadingTime;

    @PositiveOrZero
    private Integer minReadingTime;
}
