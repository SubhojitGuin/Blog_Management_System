package com.project.Blog_Management_System.Dto;

import com.project.Blog_Management_System.Deserializers.BasicHtmlSanitizationDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import tools.jackson.databind.annotation.JsonDeserialize;

@Data
public class CommentRequestDTO {
    @NotBlank
    @Size(min = 2, max = 1000)
    @JsonDeserialize(using = BasicHtmlSanitizationDeserializer.class)
    private String body;
}
