package com.project.Blog_Management_System.Controllers;

import com.project.Blog_Management_System.Dto.CategoryResponseDTO;
import com.project.Blog_Management_System.Dto.PostResponseDTO;
import com.project.Blog_Management_System.Service.Interfaces.CategoryService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/category")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Category Management", description = "Perform all category related operations")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/{slug}-{id:[0-9a-fA-F\\-]{36}}/posts")
    public ResponseEntity<Slice<PostResponseDTO>> getPostsByCategory(@PathVariable String slug,
                                                                     @PathVariable UUID id,
                                                                     @RequestParam(defaultValue = "0") Integer page,
                                                                     @RequestParam(defaultValue = "10") Integer size) {
        return new ResponseEntity<>(categoryService.getPostsByCategory(slug, id, page, size), HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponseDTO>> getAllCategories() {
        return new ResponseEntity<>(categoryService.getAllCategories(), HttpStatus.OK);
    }

    @GetMapping("/{slug}-{id:[0-9a-fA-F\\-]{36}}")
    public ResponseEntity<CategoryResponseDTO> getCategoryDetails(@PathVariable String slug,
                                                                  @PathVariable UUID id) {
        return new ResponseEntity<>(categoryService.getCategoryDetails(slug, id), HttpStatus.OK);
    }
}
