package com.project.Blog_Management_System;

import com.project.Blog_Management_System.Annotations.WithMockBlogUser;
import com.project.Blog_Management_System.Constants.ApiRoutes;
import com.project.Blog_Management_System.Dto.CategoryRequestDTO;
import com.project.Blog_Management_System.Entities.CategoryEntity;
import com.project.Blog_Management_System.Entities.PostEntity;
import com.project.Blog_Management_System.Entities.UserEntity;
import com.project.Blog_Management_System.Enums.Role;
import com.project.Blog_Management_System.Repositories.CategoryRepository;
import com.project.Blog_Management_System.Repositories.PostRepository;
import com.project.Blog_Management_System.Repositories.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AdminControllerIntegrationTest extends BaseIntegrationTest {

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
    class CreateCategory {

        @Test
        @DisplayName("Should return 201 and add category successfully")
        @WithMockBlogUser(ADMIN_USERNAME)
        void shouldReturn201AndAddCategorySuccessfully() throws Exception {
            CategoryRequestDTO categoryRequestDTO = CategoryRequestDTO.builder()
                    .name("New Category")
                    .description("This is a description for the new category.")
                    .build();

            mockMvc.perform(post(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.CATEGORY_BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(categoryRequestDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.name").value(categoryRequestDTO.getName()))
                    .andExpect(jsonPath("$.data.description").value(categoryRequestDTO.getDescription()))
                    .andExpect(jsonPath("$.data.slug").value("new-category"));

            Assertions.assertTrue(categoryRepository.findBySlug("new-category").isPresent());
        }

        @Test
        @DisplayName("Should return 409 when the category already exists")
        @WithMockBlogUser(ADMIN_USERNAME)
        void shouldReturn409WhenTheCategoryAlreadyExists() throws Exception {
            CategoryRequestDTO categoryRequestDTO = CategoryRequestDTO.builder()
                    .name(category.getName())
                    .description(category.getDescription())
                    .build();

            mockMvc.perform(post(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.CATEGORY_BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(categoryRequestDTO)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.message").value(messageService.get("exception.resource.conflict", "Category")));
        }

        @Test
        @DisplayName("Should return 400 when category input fields are null")
        @WithMockBlogUser(ADMIN_USERNAME)
        void shouldReturn400WhenCategoryInputFieldsAreNull() throws Exception {
            CategoryRequestDTO categoryRequestDTO = CategoryRequestDTO.builder()
                    .build();

            mockMvc.perform(post(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.CATEGORY_BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(categoryRequestDTO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.message").value(messageService.get("exception.validation.failed")))
                    .andExpect(jsonPath("$.error.subErrors", hasSize(2)))
                    .andExpect(jsonPath("$.error.subErrors[*].field", containsInAnyOrder("name", "description")))
                    .andExpect(jsonPath("$.error.subErrors[*].message", containsInAnyOrder(
                            messageService.get("validation.category.name.not_blank"),
                            messageService.get("validation.category.description.not_blank")
                    )));
        }

        @Test
        @DisplayName("Should return 400 when category input fails validation constraints")
        @WithMockBlogUser(ADMIN_USERNAME)
        void shouldReturn400WhenCategoryInputFails() throws Exception {
            CategoryRequestDTO categoryRequestDTO = CategoryRequestDTO.builder()
                    .name("invalid_category_name")
                    .description("This is a description for invalid category.")
                    .build();

            mockMvc.perform(post(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.CATEGORY_BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(categoryRequestDTO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.message").value(messageService.get("exception.validation.failed")))
                    .andExpect(jsonPath("$.error.subErrors", hasSize(1)))
                    .andExpect(jsonPath("$.error.subErrors[*].field", containsInAnyOrder("name")))
                    .andExpect(jsonPath("$.error.subErrors[*].message", containsInAnyOrder(
                            messageService.get("validation.category.name")
                    )));
        }

        @Test
        @DisplayName("Should return 403 when the user doesn't have ADMIN role")
        @WithMockBlogUser(USER_USERNAME)
        void shouldReturn403WhenTheUserDoesNotHaveADMINRole() throws Exception {
            CategoryRequestDTO categoryRequestDTO = CategoryRequestDTO.builder()
                    .name("New Category")
                    .description("This is a description for the new category.")
                    .build();

            mockMvc.perform(post(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.CATEGORY_BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(categoryRequestDTO)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error.message").value("Access Denied"));

            Assertions.assertFalse(categoryRepository.findBySlug("new-category").isPresent());
        }

        @Test
        @DisplayName("Should return 401 when the user is unauthorised")
        void shouldReturn401WhenTheUserIsUnauthorised() throws Exception {
            CategoryRequestDTO categoryRequestDTO = CategoryRequestDTO.builder()
                    .name("New Category")
                    .description("This is a description for the new category.")
                    .build();

            mockMvc.perform(post(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.CATEGORY_BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(categoryRequestDTO)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.message", is("Full authentication is required to access this resource")));

            Assertions.assertFalse(categoryRepository.findBySlug("new-category").isPresent());
        }
    }

    @Nested
    @DisplayName("PUT " + ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.ADMIN_CATEGORY_PATH)
    class UpdateCategory {

        @Test
        @DisplayName("Should return 200 and update category successfully")
        @WithMockBlogUser(ADMIN_USERNAME)
        void shouldReturn200AndUpdateCategorySuccessfully() throws Exception {
            CategoryRequestDTO categoryRequestDTO = CategoryRequestDTO.builder()
                    .name("Updated Category Name")
                    .description("This is a description for the updated category.")
                    .build();

            mockMvc.perform(put(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.ADMIN_CATEGORY_PATH, category.getSlug(), category.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(categoryRequestDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value(categoryRequestDTO.getName()))
                    .andExpect(jsonPath("$.data.description").value(categoryRequestDTO.getDescription()))
                    .andExpect(jsonPath("$.data.slug").value("updated-category-name"));

            Assertions.assertTrue(categoryRepository.findBySlug("updated-category-name").isPresent());
        }

        @Test
        @DisplayName("Should return 404 when the category does not exist")
        @WithMockBlogUser(ADMIN_USERNAME)
        void shouldReturn404WhenTheCategoryDoesNotExist() throws Exception {
            CategoryRequestDTO categoryRequestDTO = CategoryRequestDTO.builder()
                    .name("Updated Category Name")
                    .description("This is a description for the updated category.")
                    .build();

            mockMvc.perform(put(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.ADMIN_CATEGORY_PATH, "non-existent-slug", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(categoryRequestDTO)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.message").value(messageService.get("exception.resource.not_found", "Category")));

            Assertions.assertFalse(categoryRepository.findBySlug("updated-category-name").isPresent());
        }

        @Test
        @DisplayName("Should return 400 when category input fields are null")
        @WithMockBlogUser(ADMIN_USERNAME)
        void shouldReturn400WhenCategoryInputFieldsNull() throws Exception {
            CategoryRequestDTO categoryRequestDTO = CategoryRequestDTO.builder()
                    .build();

            mockMvc.perform(put(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.ADMIN_CATEGORY_PATH, category.getSlug(), category.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(categoryRequestDTO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.message").value(messageService.get("exception.validation.failed")))
                    .andExpect(jsonPath("$.error.subErrors", hasSize(2)))
                    .andExpect(jsonPath("$.error.subErrors[*].field", containsInAnyOrder("name", "description")))
                    .andExpect(jsonPath("$.error.subErrors[*].message", containsInAnyOrder(
                            messageService.get("validation.category.name.not_blank"),
                            messageService.get("validation.category.description.not_blank")
                    )));
        }

        @Test
        @DisplayName("Should return 400 when category input fails validation constraints")
        @WithMockBlogUser(ADMIN_USERNAME)
        void shouldReturn400WhenCategoryInputFails() throws Exception {
            CategoryRequestDTO categoryRequestDTO = CategoryRequestDTO.builder()
                    .name("invalid_updated_category_name")
                    .description("This is a description for the updated category.")
                    .build();

            mockMvc.perform(put(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.ADMIN_CATEGORY_PATH, category.getSlug(), category.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(categoryRequestDTO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.message").value(messageService.get("exception.validation.failed")))
                    .andExpect(jsonPath("$.error.subErrors", hasSize(1)))
                    .andExpect(jsonPath("$.error.subErrors[*].field", containsInAnyOrder("name")))
                    .andExpect(jsonPath("$.error.subErrors[*].message", containsInAnyOrder(
                            messageService.get("validation.category.name")
                    )));
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

            mockMvc.perform(put(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.ADMIN_CATEGORY_PATH, category.getSlug(), category.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(categoryRequestDTO)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.message").value(messageService.get("exception.resource.conflict", "Category")));

            Assertions.assertNotEquals( categoryRequestDTO.getDescription(), categoryRepository.findBySlug(category.getSlug()).get().getDescription());
        }

        @Test
        @DisplayName("Should return 403 when the user doesn't have ADMIN role")
        @WithMockBlogUser(USER_USERNAME)
        void shouldReturn403WhenTheUserDoesNotHaveADMINRole() throws Exception {
            CategoryRequestDTO categoryRequestDTO = CategoryRequestDTO.builder()
                    .name("Updated Category Name")
                    .description("This is a description for the updated category.")
                    .build();

            mockMvc.perform(put(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.ADMIN_CATEGORY_PATH, category.getSlug(), category.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(categoryRequestDTO)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error.message").value("Access Denied"));

            Assertions.assertFalse(categoryRepository.findBySlug("updated-category-name").isPresent());
        }

        @Test
        @DisplayName("Should return 401 when the user is unauthorised")
        void shouldReturn401WhenTheUserIsUnauthorised() throws Exception {
            CategoryRequestDTO categoryRequestDTO = CategoryRequestDTO.builder()
                    .name("Updated Category Name")
                    .description("This is a description for the updated category.")
                    .build();

            mockMvc.perform(put(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.ADMIN_CATEGORY_PATH, category.getSlug(), category.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(categoryRequestDTO)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.message").value("Full authentication is required to access this resource"));

            Assertions.assertFalse(categoryRepository.findBySlug("updated-category-name").isPresent());
        }
    }

    @Nested
    @DisplayName("DELETE " + ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.ADMIN_CATEGORY_PATH)
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

            Assertions.assertEquals("new-category", postRepository.findById(post.getId()).get().getCategory().getSlug());
        }

        @Test
        @DisplayName("Should return 204 when category is deleted successfully, and assigns the posts under it to uncategorised (default category if not specified)")
        @WithMockBlogUser(ADMIN_USERNAME)
        void shouldReturn204WhenCategoryIsDeletedSuccessfullyAndAssignsPostsToUncategorised() throws Exception {
            mockMvc.perform(delete(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.ADMIN_CATEGORY_PATH, category.getSlug(), category.getId()))
                    .andExpect(status().isNoContent());

            Assertions.assertEquals("uncategorised", postRepository.findById(post.getId()).get().getCategory().getSlug());
        }

        @Test
        @DisplayName("Should return 404 when the assigned category does not exist")
        @WithMockBlogUser(ADMIN_USERNAME)
        void shouldReturn404WhenTheAssignedCategoryDoesNotExist() throws Exception {
            mockMvc.perform(delete(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.ADMIN_CATEGORY_PATH, category.getSlug(), category.getId())
                            .param("newSlug", "non-existent-category"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.message").value(messageService.get("exception.resource.not_found", "Category")));

            Assertions.assertEquals(category.getSlug(), postRepository.findById(post.getId()).get().getCategory().getSlug());
        }

        @Test
        @DisplayName("Should return 404 when category doesn't exist")
        @WithMockBlogUser(ADMIN_USERNAME)
        void shouldReturn404WhenCategoryDoesNotExist() throws Exception {
            mockMvc.perform(delete(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.ADMIN_CATEGORY_PATH, "non-existent-category", UUID.randomUUID()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.message").value(messageService.get("exception.resource.not_found", "Category")));

            Assertions.assertEquals(category.getSlug(), postRepository.findById(post.getId()).get().getCategory().getSlug());
        }

        @Test
        @DisplayName("Should return 403 when user doesn't have ADMIN Role")
        @WithMockBlogUser(USER_USERNAME)
        void shouldReturn403WhenUserDoesNotHaveADMINRole() throws Exception {
            mockMvc.perform(delete(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.ADMIN_CATEGORY_PATH, category.getSlug(), category.getId()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error.message").value("Access Denied"));

            Assertions.assertEquals(category.getSlug(), postRepository.findById(post.getId()).get().getCategory().getSlug());
        }

        @Test
        @DisplayName("Should return 401 when the user is unauthorised")
        void shouldReturn401WhenTheUserIsUnauthorised() throws Exception {
            mockMvc.perform(delete(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.ADMIN_CATEGORY_PATH, category.getSlug(), category.getId()))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.message").value("Full authentication is required to access this resource"));

            Assertions.assertEquals(category.getSlug(), postRepository.findById(post.getId()).get().getCategory().getSlug());
        }

        @Test
        @DisplayName("Should return 406 when deleting uncategorised category")
        @WithMockBlogUser(ADMIN_USERNAME)
        void shouldReturn406WhenDeletingUncategorisedCategory() throws Exception {
            CategoryEntity uncategorisedCategory = categoryRepository.findBySlug("uncategorised").orElseThrow();

            mockMvc.perform(delete(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.ADMIN_CATEGORY_PATH, uncategorisedCategory.getSlug(), uncategorisedCategory.getId()))
                    .andExpect(status().isNotAcceptable())
                    .andExpect(jsonPath("$.error.message").value(messageService.get("exception.invalid.action.uncategorised_category_deletion")));
        }
    }
}
