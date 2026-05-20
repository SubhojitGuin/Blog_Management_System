package com.project.Blog_Management_System.Controllers;

import com.project.Blog_Management_System.Constants.ApiRoutes;
import com.project.Blog_Management_System.Dto.CategoryRequestDTO;
import com.project.Blog_Management_System.Dto.CategoryResponseDTO;
import com.project.Blog_Management_System.Service.Interfaces.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(ApiRoutes.ADMIN_BASE_PATH)
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Admin Operations", description = "Perform all admin related operations")
public class AdminController {

    private final CategoryService categoryService;

    @PostMapping(ApiRoutes.CATEGORY_BASE_PATH)
    @Operation(summary = "Create a New Category", description = "Creates a new category with the provided details.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Category created successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Category with the same name already exists",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access Denied",
                    content = @Content
            )
    })
    public ResponseEntity<CategoryResponseDTO> createCategory(@Valid @RequestBody CategoryRequestDTO categoryRequestDTO) {
        return new ResponseEntity<>(categoryService.createCategory(categoryRequestDTO), HttpStatus.CREATED);
    }

    @PutMapping(ApiRoutes.ADMIN_CATEGORY_PATH)
    @Operation(summary = "Update an Existing Category", description = "Updates the details of an existing category identified by its slug and ID.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Category updated successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Category with the same name already exists",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access Denied",
                    content = @Content
            )
    })
    public ResponseEntity<CategoryResponseDTO> updateCategory(@Valid @RequestBody CategoryRequestDTO categoryRequestDTO,
                                                              @PathVariable String category_slug,
                                                              @PathVariable UUID category_id) {
        return new ResponseEntity<>(categoryService.updateCategory(category_slug, category_id, categoryRequestDTO), HttpStatus.OK);
    }

    @DeleteMapping(ApiRoutes.ADMIN_CATEGORY_PATH)
    @Operation(summary = "Delete a Category", description = "Deletes an existing category identified by its slug and ID. Optionally, specify a new category slug to reassign posts from the deleted category.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Category deleted successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Category not found",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access Denied",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "406",
                    description = "Uncategorized category cannot be deleted",
                    content = @Content
            )
    })
    public ResponseEntity<Void> deleteCategory(@PathVariable String category_slug,
                                               @PathVariable UUID category_id,
                                               @RequestParam(defaultValue = "uncategorised") String newSlug) {
        categoryService.deleteCategory(category_slug, category_id, newSlug);
        return ResponseEntity.noContent().build();
    }

}
