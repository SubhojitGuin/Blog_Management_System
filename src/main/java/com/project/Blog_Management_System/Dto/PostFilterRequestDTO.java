package com.project.Blog_Management_System.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class PostFilterRequestDTO {
    @NotBlank
    private String categorySlug;

    @NotBlank
    private String title;

    @PositiveOrZero
    private Integer maxReadingTime;

    @PositiveOrZero
    private Integer minReadingTime;
}
