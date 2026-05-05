package com.project.Blog_Management_System.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CategoryRequestDTO {
    @NotBlank
    @Pattern(regexp = "^[a-zA-Z0-9\\s.-]+$", message = "Only letters, numbers, spaces, dots, and hyphens allowed")
    @Size(min = 2, max = 100)
    private String name;

    @NotBlank
    @Size(min = 10, max = 500)
    private String description;
}
