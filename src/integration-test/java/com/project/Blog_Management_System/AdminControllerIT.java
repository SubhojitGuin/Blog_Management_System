package com.project.Blog_Management_System;

import com.project.Blog_Management_System.Advice.ApiError;
import com.project.Blog_Management_System.Annotations.WithMockBlogUser;
import com.project.Blog_Management_System.Constants.ApiRoutes;
import com.project.Blog_Management_System.Dto.CategoryRequestDTO;
import com.project.Blog_Management_System.Dto.CategoryResponseDTO;
import com.project.Blog_Management_System.Entities.CategoryEntity;
import com.project.Blog_Management_System.Entities.PostEntity;
import com.project.Blog_Management_System.Entities.UserEntity;
import com.project.Blog_Management_System.Enums.Role;
import com.project.Blog_Management_System.Repositories.CategoryRepository;
import com.project.Blog_Management_System.Repositories.PostRepository;
import com.project.Blog_Management_System.Repositories.UserRepository;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Feature("Admin Category Management")
public class AdminControllerIT extends BaseIT {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    private CategoryEntity category;

    private static final String ADMIN_USERNAME = "testadmin";
    private static final String USER_USERNAME = "testuser";

    private UserEntity user;
    private UserEntity adminUser;

    @BeforeEach
    void setUp() {
        category = categoryRepository.saveAndFlush(testDataFactory.createCategory().build());

        user = userRepository.saveAndFlush(testDataFactory.createUser()
                .name("Test User")
                .username(USER_USERNAME)
                .roles(Set.of(Role.USER)).build());

        adminUser = userRepository.saveAndFlush(testDataFactory.createUser()
                .name("Test Admin")
                .username(ADMIN_USERNAME)
                .email("testadmin@gmail.com")
                .roles(Set.of(Role.ADMIN)).build());
    }

    @Nested
    @DisplayName("POST " + ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.CATEGORY_BASE_PATH)
    @Story("Admin creates category")
    @Severity(SeverityLevel.CRITICAL)
    class CreateCategory {

        @Test
        @DisplayName("Should return 201 and add category successfully")
        @WithMockBlogUser(ADMIN_USERNAME)
        void shouldReturn201AndAddCategorySuccessfully() throws Exception {
            CategoryRequestDTO categoryRequestDTO = CategoryRequestDTO.builder()
                    .name("New Category")
                    .description("This is a description for the new category.")
                    .build();

            MvcResult response = mockMvc.perform(post(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.CATEGORY_BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(categoryRequestDTO)))
                    .andExpect(status().isCreated())
                    .andReturn();

            CategoryResponseDTO categoryResponse = testResponseExtractor.extractPayload(response, CategoryResponseDTO.class);

            assertThat(categoryResponse.getName()).isEqualTo(categoryRequestDTO.getName());
            assertThat(categoryResponse.getDescription()).isEqualTo(categoryRequestDTO.getDescription());
            assertThat(categoryResponse.getSlug()).isEqualTo("new-category");

            assertThat(categoryRepository.findBySlug("new-category")).isPresent()
                    .get()
                    .satisfies(newCategory -> {
                        assertThat(newCategory.getName()).isEqualTo(categoryRequestDTO.getName());
                        assertThat(newCategory.getDescription()).isEqualTo(categoryRequestDTO.getDescription());
                        assertThat(newCategory.getSlug()).isEqualTo("new-category");
                    });
        }

        @Test
        @DisplayName("Should return 409 when the category already exists")
        @WithMockBlogUser(ADMIN_USERNAME)
        void shouldReturn409WhenTheCategoryAlreadyExists() throws Exception {
            CategoryRequestDTO categoryRequestDTO = CategoryRequestDTO.builder()
                    .name(category.getName())
                    .description(category.getDescription())
                    .build();

            MvcResult response = mockMvc.perform(post(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.CATEGORY_BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(categoryRequestDTO)))
                    .andExpect(status().isConflict())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.conflict", "Category"));
        }

        @Test
        @DisplayName("Should return 400 when category input fields are null")
        @WithMockBlogUser(ADMIN_USERNAME)
        void shouldReturn400WhenCategoryInputFieldsAreNull() throws Exception {
            CategoryRequestDTO categoryRequestDTO = CategoryRequestDTO.builder()
                    .build();

            MvcResult response = mockMvc.perform(post(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.CATEGORY_BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(categoryRequestDTO)))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.validation.failed"));
            assertThat(errorResponse.getSubErrors())
                    .hasSize(2)
                    .extracting(ApiError.FieldError::getField, ApiError.FieldError::getMessage)
                    .containsExactlyInAnyOrder(
                            tuple("name", messageService.get("validation.category.name.not_blank")),
                            tuple("description", messageService.get("validation.category.description.not_blank"))
                    );

            assertThat(categoryRepository.findBySlug("new-category")).isNotPresent();
        }

        @Test
        @DisplayName("Should return 400 when category input fails validation constraints")
        @WithMockBlogUser(ADMIN_USERNAME)
        void shouldReturn400WhenCategoryInputFails() throws Exception {
            CategoryRequestDTO categoryRequestDTO = CategoryRequestDTO.builder()
                    .name("invalid_category_name")
                    .description("This is a description for invalid category.")
                    .build();

            MvcResult response = mockMvc.perform(post(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.CATEGORY_BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(categoryRequestDTO)))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.validation.failed"));
            assertThat(errorResponse.getSubErrors())
                    .hasSize(1)
                    .extracting(ApiError.FieldError::getField, ApiError.FieldError::getMessage)
                    .containsExactlyInAnyOrder(tuple("name", messageService.get("validation.category.name")));

            assertThat(categoryRepository.findBySlug("invalid_category_name")).isNotPresent();
        }

        @Test
        @DisplayName("Should return 403 when the user doesn't have ADMIN role")
        @WithMockBlogUser(USER_USERNAME)
        void shouldReturn403WhenTheUserDoesNotHaveADMINRole() throws Exception {
            CategoryRequestDTO categoryRequestDTO = CategoryRequestDTO.builder()
                    .name("New Category")
                    .description("This is a description for the new category.")
                    .build();

            MvcResult response = mockMvc.perform(post(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.CATEGORY_BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(categoryRequestDTO)))
                    .andExpect(status().isForbidden())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo("Access Denied");

            assertThat(categoryRepository.findBySlug("new-category")).isNotPresent();
        }

        @Test
        @DisplayName("Should return 401 when the user is unauthorised")
        void shouldReturn401WhenTheUserIsUnauthorised() throws Exception {
            CategoryRequestDTO categoryRequestDTO = CategoryRequestDTO.builder()
                    .name("New Category")
                    .description("This is a description for the new category.")
                    .build();

            MvcResult response = mockMvc.perform(post(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.CATEGORY_BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(categoryRequestDTO)))
                    .andExpect(status().isUnauthorized())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo("Full authentication is required to access this resource");
            assertThat(categoryRepository.findBySlug("new-category")).isNotPresent();
        }
    }

    @Nested
    @DisplayName("PUT " + ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.ADMIN_CATEGORY_PATH)
    @Story("Admin updates category")
    @Severity(SeverityLevel.CRITICAL)
    class UpdateCategory {

        @Test
        @DisplayName("Should return 200 and update category successfully")
        @WithMockBlogUser(ADMIN_USERNAME)
        void shouldReturn200AndUpdateCategorySuccessfully() throws Exception {
            CategoryRequestDTO categoryRequestDTO = CategoryRequestDTO.builder()
                    .name("Updated Category Name")
                    .description("This is a description for the updated category.")
                    .build();

            MvcResult result = mockMvc.perform(put(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.ADMIN_CATEGORY_PATH, category.getSlug(), category.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(categoryRequestDTO)))
                    .andExpect(status().isOk())
                    .andReturn();

            CategoryResponseDTO categoryResponse = testResponseExtractor.extractPayload(result, CategoryResponseDTO.class);

            assertThat(categoryResponse.getName()).isEqualTo(categoryRequestDTO.getName());
            assertThat(categoryResponse.getDescription()).isEqualTo(categoryRequestDTO.getDescription());
            assertThat(categoryResponse.getSlug()).isEqualTo("updated-category-name");

            assertThat(categoryRepository.findBySlug("updated-category-name"))
                    .isPresent()
                    .get()
                    .satisfies(updatedCategory -> {
                        assertThat(updatedCategory.getName()).isEqualTo(categoryRequestDTO.getName());
                        assertThat(updatedCategory.getDescription()).isEqualTo(categoryRequestDTO.getDescription());
                    });
        }

        @Test
        @DisplayName("Should return 404 when the category does not exist")
        @WithMockBlogUser(ADMIN_USERNAME)
        void shouldReturn404WhenTheCategoryDoesNotExist() throws Exception {
            CategoryRequestDTO categoryRequestDTO = CategoryRequestDTO.builder()
                    .name("Updated Category Name")
                    .description("This is a description for the updated category.")
                    .build();

            MvcResult response = mockMvc.perform(put(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.ADMIN_CATEGORY_PATH, "non-existent-slug", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(categoryRequestDTO)))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.not_found", "Category"));

            assertThat(categoryRepository.findBySlug("updated-category-name")).isNotPresent();
        }

        @Test
        @DisplayName("Should return 400 when category input fields are null")
        @WithMockBlogUser(ADMIN_USERNAME)
        void shouldReturn400WhenCategoryInputFieldsNull() throws Exception {
            CategoryRequestDTO categoryRequestDTO = CategoryRequestDTO.builder()
                    .build();

            MvcResult response = mockMvc.perform(put(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.ADMIN_CATEGORY_PATH, category.getSlug(), category.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(categoryRequestDTO)))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.validation.failed"));
            assertThat(errorResponse.getSubErrors())
                    .hasSize(2)
                    .extracting(ApiError.FieldError::getField, ApiError.FieldError::getMessage)
                    .containsExactlyInAnyOrder(
                            tuple("name", messageService.get("validation.category.name.not_blank")),
                            tuple("description", messageService.get("validation.category.description.not_blank"))
                    );

            assertThat(categoryRepository.findBySlug("updated-category-name")).isNotPresent();
        }

        @Test
        @DisplayName("Should return 400 when category input fails validation constraints")
        @WithMockBlogUser(ADMIN_USERNAME)
        void shouldReturn400WhenCategoryInputFails() throws Exception {
            CategoryRequestDTO categoryRequestDTO = CategoryRequestDTO.builder()
                    .name("invalid_updated_category_name")
                    .description("This is a description for the updated category.")
                    .build();

            MvcResult response = mockMvc.perform(put(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.ADMIN_CATEGORY_PATH, category.getSlug(), category.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(categoryRequestDTO)))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.validation.failed"));
            assertThat(errorResponse.getSubErrors())
                    .hasSize(1)
                    .extracting(ApiError.FieldError::getField, ApiError.FieldError::getMessage)
                    .containsExactlyInAnyOrder(tuple("name", messageService.get("validation.category.name")));

            assertThat(categoryRepository.findBySlug("updated-category-name")).isNotPresent();
        }

        @Test
        @DisplayName("Should return 409 when the updated category exists")
        @WithMockBlogUser(ADMIN_USERNAME)
        void shouldReturn409WhenUpdatedCategoryExists() throws Exception {
            CategoryEntity category1 = categoryRepository.saveAndFlush(testDataFactory.createCategory().name("Existing Category").build());

            CategoryRequestDTO categoryRequestDTO = CategoryRequestDTO.builder()
                    .name(category1.getName())
                    .description("This is a description for the updated category.")
                    .build();

            MvcResult response = mockMvc.perform(put(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.ADMIN_CATEGORY_PATH, category.getSlug(), category.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(categoryRequestDTO)))
                    .andExpect(status().isConflict())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.conflict", "Category"));

            assertThat(categoryRepository.findBySlug(category.getSlug())).isPresent()
                    .get()
                    .satisfies(existingCategory -> {
                        assertThat(existingCategory.getName()).isEqualTo(category.getName());
                        assertThat(existingCategory.getDescription()).isEqualTo(category.getDescription());
                    });
        }

        @Test
        @DisplayName("Should return 403 when the user doesn't have ADMIN role")
        @WithMockBlogUser(USER_USERNAME)
        void shouldReturn403WhenTheUserDoesNotHaveADMINRole() throws Exception {
            CategoryRequestDTO categoryRequestDTO = CategoryRequestDTO.builder()
                    .name("Updated Category Name")
                    .description("This is a description for the updated category.")
                    .build();

            MvcResult response = mockMvc.perform(put(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.ADMIN_CATEGORY_PATH, category.getSlug(), category.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(categoryRequestDTO)))
                    .andExpect(status().isForbidden())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo("Access Denied");

            assertThat(categoryRepository.findBySlug("updated-category-name")).isNotPresent();
        }

        @Test
        @DisplayName("Should return 401 when the user is unauthorised")
        void shouldReturn401WhenTheUserIsUnauthorised() throws Exception {
            CategoryRequestDTO categoryRequestDTO = CategoryRequestDTO.builder()
                    .name("Updated Category Name")
                    .description("This is a description for the updated category.")
                    .build();

            MvcResult response = mockMvc.perform(put(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.ADMIN_CATEGORY_PATH, category.getSlug(), category.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(categoryRequestDTO)))
                    .andExpect(status().isUnauthorized())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo("Full authentication is required to access this resource");

            assertThat(categoryRepository.findBySlug("updated-category-name")).isNotPresent();
        }
    }

    @Nested
    @DisplayName("DELETE " + ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.ADMIN_CATEGORY_PATH)
    @Story("Admin deletes category")
    @Severity(SeverityLevel.CRITICAL)
    class DeleteCategory {

        private PostEntity post;

        @BeforeEach
        void setUpPostsUnderCategory() {
            post = postRepository.saveAndFlush(testDataFactory.createCustomPost(adminUser, category));
        }

        @Test
        @DisplayName("Should return 204 when category is deleted successfully, and assigns the posts under it to a new category")
        @WithMockBlogUser(ADMIN_USERNAME)
        void shouldReturn204WhenCategoryIsDeletedSuccessfullyAndAssignsPostsToNewCategory() throws Exception {
            CategoryEntity newCategory = categoryRepository.saveAndFlush(testDataFactory.createCategory().name("New Category").build());

            mockMvc.perform(delete(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.ADMIN_CATEGORY_PATH, category.getSlug(), category.getId())
                            .param("newSlug", "new-category"))
                    .andExpect(status().isNoContent());

            assertThat(postRepository.findById(post.getId()))
                    .isPresent()
                    .get()
                    .satisfies(deletedPost -> {
                        assertThat(deletedPost.getCategory().getSlug()).isEqualTo("new-category");
                    });
        }

        @Test
        @DisplayName("Should return 204 when category is deleted successfully, and assigns the posts under it to uncategorised (default category if not specified)")
        @WithMockBlogUser(ADMIN_USERNAME)
        void shouldReturn204WhenCategoryIsDeletedSuccessfullyAndAssignsPostsToUncategorised() throws Exception {
            mockMvc.perform(delete(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.ADMIN_CATEGORY_PATH, category.getSlug(), category.getId()))
                    .andExpect(status().isNoContent());

            assertThat(postRepository.findById(post.getId()))
                    .isPresent()
                    .get()
                    .satisfies(deletedPost -> {
                        assertThat(deletedPost.getCategory().getSlug()).isEqualTo("uncategorised");
                    });
        }

        @Test
        @DisplayName("Should return 404 when the assigned category does not exist")
        @WithMockBlogUser(ADMIN_USERNAME)
        void shouldReturn404WhenTheAssignedCategoryDoesNotExist() throws Exception {
            MvcResult response = mockMvc.perform(delete(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.ADMIN_CATEGORY_PATH, category.getSlug(), category.getId())
                            .param("newSlug", "non-existent-category"))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.not_found", "Category"));

            assertThat(postRepository.findById(post.getId())).isPresent()
                    .get()
                    .satisfies(deletedPost -> {
                        assertThat(deletedPost.getCategory().getSlug()).isEqualTo(category.getSlug());
                    });
        }

        @Test
        @DisplayName("Should return 404 when category doesn't exist")
        @WithMockBlogUser(ADMIN_USERNAME)
        void shouldReturn404WhenCategoryDoesNotExist() throws Exception {
            MvcResult response = mockMvc.perform(delete(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.ADMIN_CATEGORY_PATH, "non-existent-category", UUID.randomUUID()))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.not_found", "Category"));
            assertThat(postRepository.findById(post.getId()))
                    .isPresent()
                    .get()
                    .satisfies(deletedPost -> {
                        assertThat(deletedPost.getCategory().getSlug()).isEqualTo(category.getSlug());
                    });
        }

        @Test
        @DisplayName("Should return 403 when user doesn't have ADMIN Role")
        @WithMockBlogUser(USER_USERNAME)
        void shouldReturn403WhenUserDoesNotHaveADMINRole() throws Exception {
            MvcResult response = mockMvc.perform(delete(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.ADMIN_CATEGORY_PATH, category.getSlug(), category.getId()))
                    .andExpect(status().isForbidden())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo("Access Denied");

            assertThat(postRepository.findById(post.getId())).isPresent()
                    .get()
                    .satisfies(deletedPost -> {
                        assertThat(deletedPost.getCategory().getSlug()).isEqualTo(category.getSlug());
                    });
        }

        @Test
        @DisplayName("Should return 401 when the user is unauthorised")
        void shouldReturn401WhenTheUserIsUnauthorised() throws Exception {
            MvcResult response = mockMvc.perform(delete(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.ADMIN_CATEGORY_PATH, category.getSlug(), category.getId()))
                    .andExpect(status().isUnauthorized())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo("Full authentication is required to access this resource");

            assertThat(postRepository.findById(post.getId())).isPresent()
                    .get()
                    .satisfies(deletedPost -> {
                        assertThat(deletedPost.getCategory().getSlug()).isEqualTo(category.getSlug());
                    });
        }

        @Test
        @DisplayName("Should return 406 when deleting uncategorised category")
        @WithMockBlogUser(ADMIN_USERNAME)
        void shouldReturn406WhenDeletingUncategorisedCategory() throws Exception {
            CategoryEntity uncategorisedCategory = categoryRepository.findBySlug("uncategorised").orElseThrow();

            MvcResult response = mockMvc.perform(delete(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.ADMIN_CATEGORY_PATH, uncategorisedCategory.getSlug(), uncategorisedCategory.getId()))
                    .andExpect(status().isNotAcceptable())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.invalid.action.uncategorised_category_deletion"));
        }
    }
}
