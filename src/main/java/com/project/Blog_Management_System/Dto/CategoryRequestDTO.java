package com.project.Blog_Management_System.Dto;

import com.project.Blog_Management_System.Constants.RegexConstants;
import com.project.Blog_Management_System.Deserializers.BasicHtmlSanitizationDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonDeserialize;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryRequestDTO {
    @NotBlank(message = "{validation.category.name.not_blank}")
    @Pattern(regexp = RegexConstants.CATEGORY_NAME, message = "{validation.category.name}")
    @Size(min = 2, max = 100, message = "{validation.category.name.size}")
    private String name;

    @NotBlank(message = "{validation.category.description.not_blank}")
    @Size(min = 10, max = 500, message = "{validation.category.description.size}")
    @JsonDeserialize(using = BasicHtmlSanitizationDeserializer.class)
    private String description;
}
