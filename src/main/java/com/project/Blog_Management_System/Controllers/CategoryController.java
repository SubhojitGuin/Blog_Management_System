package com.project.Blog_Management_System.Controllers;

import com.project.Blog_Management_System.Constants.ApiRoutes;
import com.project.Blog_Management_System.Dto.CategoryResponseDTO;
import com.project.Blog_Management_System.Dto.PostResponseDTO;
import com.project.Blog_Management_System.Service.Interfaces.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping(ApiRoutes.CATEGORY_BASE_PATH)
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Category Management", description = "Perform all category related operations")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping(ApiRoutes.CATEGORY_POSTS)
    @Operation(summary = "Get Posts by Category", description = "Retrieves a paginated list of posts belonging to a specific category identified by its slug and ID.")
    public ResponseEntity<Slice<PostResponseDTO>> getPostsByCategory(@PathVariable String category_slug,
                                                                     @PathVariable UUID category_id,
                                                                     @RequestParam(required = false) UUID post_cursor,
                                                                     @RequestParam(defaultValue = "10") Integer size) {
        return new ResponseEntity<>(categoryService.getPostsByCategory(category_slug, category_id, post_cursor, size), HttpStatus.OK);
    }

    @GetMapping
    @Operation(summary = "Get All Categories", description = "Retrieves a list of all available categories.")
    public ResponseEntity<List<CategoryResponseDTO>> getAllCategories() {
        return new ResponseEntity<>(categoryService.getAllCategories(), HttpStatus.OK);
    }

    @GetMapping(ApiRoutes.CATEGORY_PATH_VARIABLE)
    @Operation(summary = "Get Category Details", description = "Retrieves the details of a specific category by its slug and ID.")
    public ResponseEntity<CategoryResponseDTO> getCategoryDetails(@PathVariable String category_slug,
                                                                  @PathVariable UUID category_id) {
        return new ResponseEntity<>(categoryService.getCategoryDetails(category_slug, category_id), HttpStatus.OK);
    }
}
