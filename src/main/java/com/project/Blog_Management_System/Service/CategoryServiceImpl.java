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
    public Slice<PostResponseDTO> getPostsByCategory(String slug, UUID id, Integer page, Integer size) {
        UserEntity user = getCurrentUser();

        CategoryEntity category = categoryRepository.findById(id).orElse(null);
        isInvalidCategory(category, slug);

        return postRepository.findPostsByCategory(category, user, PageRequest.of(page, size));
    }

    @Override
    public List<CategoryResponseDTO> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(category -> modelMapper.map(category, CategoryResponseDTO.class))
                .toList();
    }

    @Override
    public CategoryResponseDTO getCategoryDetails(String slug, UUID id) {
        CategoryEntity category = categoryRepository.findById(id).orElse(null);
        isInvalidCategory(category, slug);

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
    public CategoryResponseDTO updateCategory(String slug, UUID id, CategoryRequestDTO categoryRequestDTO) {
        CategoryEntity category = categoryRepository.findById(id).orElse(null);
        isInvalidCategory(category, slug);

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
    public void deleteCategory(String slug, UUID id, String newSlug) {
        if (slug.equals("uncategorised")) {
            throw new InvalidActionException("Uncategorised category can't be deleted");
        }

        CategoryEntity oldCategory = categoryRepository.findById(id).orElse(null);
        isInvalidCategory(oldCategory, slug);

        CategoryEntity newCategory = categoryRepository.findBySlug(newSlug).orElse(null);
        isInvalidCategory(newCategory, newSlug);

        postRepository.updatePostsCategory(oldCategory, newCategory);
        log.info("ADMIN DELETED OLD CATEGORY: {} AND UPDATED WITH NEW CATEGORY: {}", oldCategory.getName(), newCategory.getName());
        categoryRepository.delete(oldCategory);
    }

}
