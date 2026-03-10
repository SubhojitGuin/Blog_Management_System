package com.project.Blog_Management_System.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class CategoryResponseDTO {
    private UUID id;
    private String slug;
    private String name;
    private String description;
}
