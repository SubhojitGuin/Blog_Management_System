package com.project.Blog_Management_System.Dto;

import com.project.Blog_Management_System.Deserializers.BasicHtmlSanitizationDeserializer;
import com.project.Blog_Management_System.Deserializers.CustomHtmlSanitizationDeserializer;
import com.project.Blog_Management_System.Deserializers.StringSanitizationDeserializer;
import com.project.Blog_Management_System.Enums.PostStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.time.LocalDateTime;

@Data
public class PostRequestDTO {
    @NotBlank
    @Size(min = 2, max = 100)
    @JsonDeserialize(using = StringSanitizationDeserializer.class)
    private String title;

    @NotBlank
    @Size(min = 50, max = 2000)
    @JsonDeserialize(using = BasicHtmlSanitizationDeserializer.class)
    private String description;

    @NotBlank
    @Size(min = 100, max = 125000)
    @JsonDeserialize(using = CustomHtmlSanitizationDeserializer.class)
    private String content;

    @NotBlank
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug must be lowercase alphanumeric with hyphens only")
    @Size(min = 2, max = 100)
    private String categorySlug;

    private PostStatus status = PostStatus.DRAFT;

    @Future(message = "publish must be at a future date and time")
    private LocalDateTime publishAt;
}
