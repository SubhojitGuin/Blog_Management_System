package com.project.Blog_Management_System;

import com.project.Blog_Management_System.Advice.ApiError;
import com.project.Blog_Management_System.Annotations.WithMockBlogUser;
import com.project.Blog_Management_System.Constants.ApiRoutes;
import com.project.Blog_Management_System.Dto.CategoryResponseDTO;
import com.project.Blog_Management_System.Dto.PostResponseDTO;
import com.project.Blog_Management_System.Entities.CategoryEntity;
import com.project.Blog_Management_System.Entities.PostEntity;
import com.project.Blog_Management_System.Entities.UserEntity;
import com.project.Blog_Management_System.Repositories.CategoryRepository;
import com.project.Blog_Management_System.Repositories.PostRepository;
import com.project.Blog_Management_System.Repositories.UserRepository;
import com.project.Blog_Management_System.Utils.TestSliceResponse;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Feature("Category Retrieval Operations")
public class CategoryControllerIT extends BaseIT {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    private CategoryEntity category;

    private UserEntity user;
    private static final String USERNAME = "testuser";

    @BeforeEach
    public void setup() {
        category = categoryRepository.saveAndFlush(testDataFactory.createCategory().build());
        user = userRepository.saveAndFlush(testDataFactory.createUser().username(USERNAME).build());
    }

    @Nested
    @DisplayName("GET " + ApiRoutes.CATEGORY_BASE_PATH + ApiRoutes.CATEGORY_POSTS)
    @Story("User retrieves posts by category")
    @Severity(SeverityLevel.NORMAL)
    class GetPostsByCategory {

        private PostEntity post;

        @BeforeEach
        void addPostsToCategory() {
            post = postRepository.saveAndFlush(testDataFactory.createCustomPost(user, category));
        }

        @Test
        @DisplayName("Should return 200 and posts for a valid category")
        @WithMockBlogUser(USERNAME)
        void shouldReturnPostsForValidCategory() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.CATEGORY_BASE_PATH + ApiRoutes.CATEGORY_POSTS, category.getSlug(), category.getId())
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andReturn();

            TestSliceResponse<PostResponseDTO> sliceResponse = testResponseExtractor.extractSlicePayload(response, PostResponseDTO.class);

            assertThat(sliceResponse.getContent())
                    .hasSize(1)
                    .satisfies(posts -> {
                        assertThat(posts.getFirst().getTitle()).isEqualTo(post.getTitle());
                        assertThat(posts.getFirst().getCategory().getId()).isEqualTo(category.getId());
                    });
            assertThat(sliceResponse.isEmpty()).isFalse();
            assertThat(sliceResponse.isFirst()).isTrue();
            assertThat(sliceResponse.isLast()).isTrue();
            assertThat(sliceResponse.getNumberOfElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return 404 for a non-existent category")
        @WithMockBlogUser(USERNAME)
        void shouldReturn404ForNonExistentCategory() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.CATEGORY_BASE_PATH + ApiRoutes.CATEGORY_POSTS, "non-existent-slug", UUID.randomUUID())
                            .param("size", "10"))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.not_found", "Category"));
        }

        @Test
        @DisplayName("Should return 200 and no posts for a category with no posts")
        @WithMockBlogUser(USERNAME)
        void shouldReturnNoPostsForValidCategory() throws Exception {
            CategoryEntity category = categoryRepository.saveAndFlush(CategoryEntity.builder()
                    .name("Test Category 1")
                    .description("This is a test category 1")
                    .build());

            MvcResult response = mockMvc.perform(get(ApiRoutes.CATEGORY_BASE_PATH + ApiRoutes.CATEGORY_POSTS, category.getSlug(), category.getId())
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andReturn();

            TestSliceResponse<PostResponseDTO> sliceResponse = testResponseExtractor.extractSlicePayload(response, PostResponseDTO.class);

            assertThat(sliceResponse.getContent())
                    .hasSize(0);

            assertThat(sliceResponse.isEmpty()).isTrue();
            assertThat(sliceResponse.isFirst()).isTrue();
            assertThat(sliceResponse.isLast()).isTrue();
            assertThat(sliceResponse.getNumberOfElements()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should return 404 when the slug and id do not belong to the same category")
        @WithMockBlogUser(USERNAME)
        void  shouldReturn404WhenTheSlugAndIdBelongToTheSameCategory() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.CATEGORY_BASE_PATH + ApiRoutes.CATEGORY_POSTS, "non-existent-slug", category.getId())
                            .param("size", "10"))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.not_found", "Category"));
        }

        @Test
        @DisplayName("Should return 200 and paginated slice of post when post cursor is used")
        @WithMockBlogUser(USERNAME)
        void shouldReturnPaginatedSlicesOfPostWhenPostCursorIsUsed() throws Exception {
            PostEntity post2 = postRepository.saveAndFlush(testDataFactory.createCustomPost(user, category));
            PostEntity post3 = postRepository.saveAndFlush(testDataFactory.createCustomPost(user, category));

            MvcResult response = mockMvc.perform(get(ApiRoutes.CATEGORY_BASE_PATH + ApiRoutes.CATEGORY_POSTS, category.getSlug(), category.getId())
                            .param("size", "1")
                            .param("post_cursor", post3.getId().toString()))
                    .andExpect(status().isOk())
                    .andReturn();

            TestSliceResponse<PostResponseDTO> sliceResponse = testResponseExtractor.extractSlicePayload(response, PostResponseDTO.class);

            assertThat(sliceResponse.getContent())
                    .hasSize(1)
                    .satisfies(posts -> {
                        assertThat(posts.getFirst().getId()).isEqualTo(post2.getId());
                        assertThat(posts.getFirst().getCategory().getId()).isEqualTo(category.getId());
                    });
            assertThat(sliceResponse.isEmpty()).isFalse();
            assertThat(sliceResponse.isFirst()).isTrue();
            assertThat(sliceResponse.isLast()).isFalse();
            assertThat(sliceResponse.getNumberOfElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return 401 for unauthenticated user")
        void shouldReturn401ForUnauthenticatedUser() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.CATEGORY_BASE_PATH + ApiRoutes.CATEGORY_POSTS, category.getSlug(), category.getId())
                            .param("size", "10"))
                    .andExpect(status().isUnauthorized())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo("Full authentication is required to access this resource");
        }
    }

    @Nested
    @DisplayName("GET " + ApiRoutes.CATEGORY_BASE_PATH)
    @Story("User gets all categories")
    @Severity(SeverityLevel.NORMAL)
    class GetAllCategories {

        @Test
        @DisplayName("Should return 200 and all categories")
        @WithMockBlogUser(USERNAME)
        void shouldReturnAllCategories() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.CATEGORY_BASE_PATH))
                    .andExpect(status().isOk())
                    .andReturn();

            List<CategoryResponseDTO> categoriesList = testResponseExtractor.extractListPayload(response, CategoryResponseDTO.class);
            assertThat(categoriesList)
                    .hasSize(2)
                    .satisfies(categories -> {
                        assertThat(categories.get(1).getId()).isEqualTo(category.getId());
                        assertThat(categories.get(1).getName()).isEqualTo(category.getName());
                        assertThat(categories.get(1).getSlug()).isEqualTo(category.getSlug());
                        assertThat(categories.get(1).getDescription()).isEqualTo(category.getDescription());
                    });
        }

        @Test
        @DisplayName("Should return 401 for unauthenticated user")
        void shouldReturn401ForUnauthenticatedUser() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.CATEGORY_BASE_PATH))
                    .andExpect(status().isUnauthorized())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo("Full authentication is required to access this resource");
        }
    }

    @Nested
    @DisplayName("GET " + ApiRoutes.CATEGORY_BASE_PATH + ApiRoutes.CATEGORY_PATH_VARIABLE)
    @Story("User retrieves category details")
    @Severity(SeverityLevel.NORMAL)
    class GetCategoryDetails {

        @Test
        @DisplayName("Should return 200 and category details for a valid category")
        @WithMockBlogUser(USERNAME)
        void shouldReturnCategoryDetailsForValidCategory() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.CATEGORY_BASE_PATH + ApiRoutes.CATEGORY_PATH_VARIABLE, category.getSlug(), category.getId()))
                    .andExpect(status().isOk())
                    .andReturn();

            CategoryResponseDTO categoryResponseDTO = testResponseExtractor.extractPayload(response, CategoryResponseDTO.class);
            assertThat(categoryResponseDTO.getId()).isEqualTo(category.getId());
            assertThat(categoryResponseDTO.getName()).isEqualTo(category.getName());
            assertThat(categoryResponseDTO.getDescription()).isEqualTo(category.getDescription());
            assertThat(categoryResponseDTO.getSlug()).isEqualTo(category.getSlug());
        }

        @Test
        @DisplayName("Should return 404 for a non-existent category")
        @WithMockBlogUser(USERNAME)
        void shouldReturn404ForNonExistentCategory() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.CATEGORY_BASE_PATH + ApiRoutes.CATEGORY_PATH_VARIABLE, "non-existent-slug", UUID.randomUUID()))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.not_found", "Category"));
        }

        @Test
        @DisplayName("Should return 404 when the slug and id do not belong to the same category")
        @WithMockBlogUser(USERNAME)
        void shouldReturn404WhenTheSlugAndIdBelongToTheSameCategory() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.CATEGORY_BASE_PATH + ApiRoutes.CATEGORY_PATH_VARIABLE, "non-existent-slug", category.getId()))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.not_found", "Category"));
        }

        @Test
        @DisplayName("Should return 401 for unauthenticated user")
        void shouldReturn401ForUnauthenticatedUser() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.CATEGORY_BASE_PATH + ApiRoutes.CATEGORY_PATH_VARIABLE, category.getSlug(), category.getId()))
                    .andExpect(status().isUnauthorized())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo("Full authentication is required to access this resource");
        }
    }
}
