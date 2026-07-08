package com.project.Blog_Management_System.Dto;

import com.project.Blog_Management_System.Constants.RegexConstants;
import com.project.Blog_Management_System.Deserializers.BasicHtmlSanitizationDeserializer;
import com.project.Blog_Management_System.Deserializers.CustomHtmlSanitizationDeserializer;
import com.project.Blog_Management_System.Deserializers.StringSanitizationDeserializer;
import com.project.Blog_Management_System.Enums.PostStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PostRequestDTO {

    public static final int TITLE_MIN = 2;
    public static final int TITLE_MAX = 100;
    public static final int DESCRIPTION_MIN = 50;
    public static final int DESCRIPTION_MAX = 2000;
    public static final int CONTENT_MIN = 100;
    public static final int CONTENT_MAX = 125000;
    public static final int CATEGORY_MIN = 2;
    public static final int CATEGORY_MAX = 100;

    @NotBlank(message = "{validation.post.title.not_blank}")
    @Size(min = TITLE_MIN, max = TITLE_MAX, message = "{validation.post.title.size}")
    @JsonDeserialize(using = StringSanitizationDeserializer.class)
    private String title;

    @NotBlank(message = "{validation.post.description.not_blank}")
    @Size(min = DESCRIPTION_MIN, max = DESCRIPTION_MAX, message = "{validation.post.description.size}")
    @JsonDeserialize(using = BasicHtmlSanitizationDeserializer.class)
    private String description;

    @NotBlank(message = "{validation.post.content.not_blank}")
    @Size(min = CONTENT_MIN, max = CONTENT_MAX, message = "{validation.post.content.size}")
    @JsonDeserialize(using = CustomHtmlSanitizationDeserializer.class)
    private String content;

    @Pattern(regexp = RegexConstants.CATEGORY_SLUG, message = "{validation.category.slug}")
    @Size(min = CATEGORY_MIN, max = CATEGORY_MAX, message = "{validation.category.slug.size}")
    private String categorySlug = "uncategorised";

    private PostStatus status = PostStatus.DRAFT;

    @Future(message = "{validation.post.publish_at.future}")
    private LocalDateTime publishAt;
}
