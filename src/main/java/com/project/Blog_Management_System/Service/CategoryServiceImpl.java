package com.project.Blog_Management_System.Service;

import com.project.Blog_Management_System.Dto.CategoryRequestDTO;
import com.project.Blog_Management_System.Dto.CategoryResponseDTO;
import com.project.Blog_Management_System.Dto.PostResponseDTO;
import com.project.Blog_Management_System.Entities.CategoryEntity;
import com.project.Blog_Management_System.Entities.UserEntity;
import com.project.Blog_Management_System.Exceptions.InvalidActionException;
import com.project.Blog_Management_System.Exceptions.ResourceConflictException;
import com.project.Blog_Management_System.Repositories.CategoryRepository;
import com.project.Blog_Management_System.Repositories.PostRepository;
import com.project.Blog_Management_System.Service.Interfaces.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.project.Blog_Management_System.Utils.AppUtils.generateSlug;
import static com.project.Blog_Management_System.Utils.AppUtils.getCurrentUser;
import static com.project.Blog_Management_System.Utils.ValidationUtils.isInvalidCategory;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final ModelMapper modelMapper;

    @Override
    @Transactional(readOnly = true)
    public Slice<PostResponseDTO> getPostsByCategory(String categorySlug, UUID categoryId, UUID postCursor, Integer size) {
        UserEntity user = getCurrentUser();

        CategoryEntity category = categoryRepository.findById(categoryId).orElse(null);
        isInvalidCategory(category, categorySlug);

        return postRepository.findPostsByCategory(categoryId, postCursor, user.getId(), PageRequest.of(0, size));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponseDTO> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(category -> modelMapper.map(category, CategoryResponseDTO.class))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponseDTO getCategoryDetails(String categorySlug, UUID categoryId) {
        CategoryEntity category = categoryRepository.findById(categoryId).orElse(null);
        isInvalidCategory(category, categorySlug);

        return modelMapper.map(category, CategoryResponseDTO.class);
    }

    @Override
    @Transactional
    public CategoryResponseDTO createCategory(CategoryRequestDTO categoryRequestDTO) {
        String slug = generateSlug(categoryRequestDTO.getName());

        if (categoryRepository.findBySlug(slug).isPresent()) {
            throw new ResourceConflictException("Category already exists");
        }

        CategoryEntity category = modelMapper.map(categoryRequestDTO, CategoryEntity.class);
        category.setSlug(slug);

        CategoryEntity savedCategory = categoryRepository.saveAndFlush(category);

        log.info("ADMIN CREATED CATEGORY {}", savedCategory.getName());

        return modelMapper.map(savedCategory, CategoryResponseDTO.class);
    }

    @Override
    @Transactional
    public CategoryResponseDTO updateCategory(String categorySlug, UUID categoryId, CategoryRequestDTO categoryRequestDTO) {
        CategoryEntity category = categoryRepository.findById(categoryId).orElse(null);
        isInvalidCategory(category, categorySlug);

        String newSlug = generateSlug(categoryRequestDTO.getName());
        if (categoryRepository.findBySlug(newSlug).isPresent()) {
            throw new ResourceConflictException("Category already exists");
        }

        modelMapper.map(categoryRequestDTO, category);
        category.setSlug(newSlug);
        categoryRepository.saveAndFlush(category);

        log.info("ADMIN UPDATED CATEGORY: {}", category.getName());
        return modelMapper.map(category, CategoryResponseDTO.class);
    }


    @Override
    @Transactional
    public void deleteCategory(String oldSlug, UUID categoryId, String newSlug) {
        if (oldSlug.equals("uncategorised")) {
            throw new InvalidActionException("Uncategorised category can't be deleted");
        }

        CategoryEntity oldCategory = categoryRepository.findById(categoryId).orElse(null);
        isInvalidCategory(oldCategory, oldSlug);

        CategoryEntity newCategory = categoryRepository.findBySlug(newSlug).orElse(null);
        isInvalidCategory(newCategory, newSlug);

        postRepository.updatePostsCategory(oldCategory, newCategory);
        log.info("ADMIN DELETED OLD CATEGORY: {} AND UPDATED WITH NEW CATEGORY: {}", oldCategory.getName(), newCategory.getName());
        categoryRepository.delete(oldCategory);
    }

}
