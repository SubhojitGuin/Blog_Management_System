package com.project.Blog_Management_System.Dto;

import com.project.Blog_Management_System.Deserializers.BasicHtmlSanitizationDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import tools.jackson.databind.annotation.JsonDeserialize;

@Data
public class CommentRequestDTO {
    @NotBlank(message = "{validation.comment.body.not_blank}")
    @Size(min = 1, max = 1000, message = "{validation.comment.body.size}")
    @JsonDeserialize(using = BasicHtmlSanitizationDeserializer.class)
    private String body;
}
