package com.project.Blog_Management_System.Controllers;

import com.project.Blog_Management_System.Dto.CategoryRequestDTO;
import com.project.Blog_Management_System.Dto.CategoryResponseDTO;
import com.project.Blog_Management_System.Service.Interfaces.CategoryService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Admin Operations", description = "Perform all admin related operations")
public class AdminController {

    private final CategoryService categoryService;

    @PostMapping("/category")
    public ResponseEntity<CategoryResponseDTO> createCategory(@Valid @RequestBody CategoryRequestDTO categoryRequestDTO) {
        return new ResponseEntity<>(categoryService.createCategory(categoryRequestDTO), HttpStatus.CREATED);
    }

    @PutMapping("/category/{slug}-{id:[0-9a-fA-F\\-]{36}}")
    public ResponseEntity<CategoryResponseDTO> updateCategory(@Valid @RequestBody CategoryRequestDTO categoryRequestDTO,
                                                              @PathVariable String slug,
                                                              @PathVariable UUID id) {
        return new ResponseEntity<>(categoryService.updateCategory(slug, id, categoryRequestDTO), HttpStatus.OK);
    }

    @DeleteMapping("/category/{slug}-{id:[0-9a-fA-F\\-]{36}}")
    public ResponseEntity<Void> deleteCategory(@PathVariable String slug,
                                               @PathVariable UUID id,
                                               @RequestParam(defaultValue = "uncategorised") String newSlug) {
        categoryService.deleteCategory(slug, id, newSlug);
        return ResponseEntity.noContent().build();
    }

}
