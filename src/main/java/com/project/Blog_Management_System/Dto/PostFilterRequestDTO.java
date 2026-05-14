package com.project.Blog_Management_System.Dto;

import com.project.Blog_Management_System.Constants.RegexConstants;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PostFilterRequestDTO {
    @Pattern(regexp = RegexConstants.CATEGORY_SLUG, message = "{validation.category.slug}")
    @Size(min = 2, max = 100, message = "{validation.category.slug.size}")
    private String categorySlug;

    @Size(min = 1, max = 100, message = "{validation.post.title.size}")
    private String title;

    @PositiveOrZero(message = "{validation.post.reading_time.positive_or_zero}")
    private Integer maxReadingTime;

    @PositiveOrZero(message = "{validation.post.reading_time.positive_or_zero}")
    private Integer minReadingTime;
}
