package com.project.Blog_Management_System.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CategoryRequestDTO {
    private String name;
    private String description;
}
