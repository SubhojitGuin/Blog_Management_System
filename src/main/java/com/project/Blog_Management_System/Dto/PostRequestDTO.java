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
    @NotBlank(message = "{validation.post.title.not_blank}")
    @Size(min = 2, max = 100, message = "{validation.post.title.size}")
    @JsonDeserialize(using = StringSanitizationDeserializer.class)
    private String title;

    @NotBlank(message = "{validation.post.description.not_blank}")
    @Size(min = 50, max = 2000, message = "{validation.post.description.size}")
    @JsonDeserialize(using = BasicHtmlSanitizationDeserializer.class)
    private String description;

    @NotBlank(message = "{validation.post.content.not_blank}")
    @Size(min = 100, max = 125000, message = "{validation.post.content.size}")
    @JsonDeserialize(using = CustomHtmlSanitizationDeserializer.class)
    private String content;

    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "{validation.category.slug}")
    @Size(min = 2, max = 100, message = "{validation.category.slug.size}")
    private String categorySlug = "uncategorised";

    private PostStatus status = PostStatus.DRAFT;

    @Future(message = "{validation.post.publish_at.future}")
    private LocalDateTime publishAt;
}
