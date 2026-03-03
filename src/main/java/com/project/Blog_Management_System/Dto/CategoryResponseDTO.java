package com.project.Blog_Management_System.Dto;

import lombok.Data;

@Data
public class CategoryResponseDTO {
    private String id;
    private String slug;
    private String name;
    private String description;
}
