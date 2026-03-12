package com.project.Blog_Management_System.Service.Interfaces;

import com.project.Blog_Management_System.Dto.CategoryRequestDTO;
import com.project.Blog_Management_System.Dto.CategoryResponseDTO;
import com.project.Blog_Management_System.Dto.PostResponseDTO;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.UUID;

public interface CategoryService {

    Slice<PostResponseDTO> getPostsByCategory(String slug, UUID id, Integer page, Integer size);

    List<CategoryResponseDTO> getAllCategories();

    CategoryResponseDTO getCategoryDetails(String slug, UUID id);

    CategoryResponseDTO createCategory(CategoryRequestDTO categoryRequestDTO);

    CategoryResponseDTO updateCategory(String slug, UUID id, CategoryRequestDTO categoryRequestDTO);

    void deleteCategory(String slug, UUID id, String newSlug);

}
