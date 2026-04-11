package com.project.Blog_Management_System.Dto;

import lombok.Data;

@Data
public class PostFilterRequestDTO {
    String categorySlug;
    String title;
}
