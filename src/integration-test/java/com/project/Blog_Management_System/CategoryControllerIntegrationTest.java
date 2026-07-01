package com.project.Blog_Management_System;

import com.project.Blog_Management_System.Annotations.WithMockBlogUser;
import com.project.Blog_Management_System.Constants.ApiRoutes;
import com.project.Blog_Management_System.Entities.CategoryEntity;
import com.project.Blog_Management_System.Entities.PostEntity;
import com.project.Blog_Management_System.Entities.UserEntity;
import com.project.Blog_Management_System.Repositories.CategoryRepository;
import com.project.Blog_Management_System.Repositories.PostRepository;
import com.project.Blog_Management_System.Repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CategoryControllerIntegrationTest extends BaseIntegrationTest {

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
    class GetPostsByCategoryTests {

        private PostEntity post;

        @BeforeEach
        void addPostsToCategory() {
            post = postRepository.saveAndFlush(testDataFactory.createCustomPost(user, category));
        }

        @Test
        @DisplayName("Should return 200 and posts for a valid category")
        @WithMockBlogUser(USERNAME)
        void shouldReturnPostsForValidCategory() throws Exception {
            mockMvc.perform(get(ApiRoutes.CATEGORY_BASE_PATH + ApiRoutes.CATEGORY_POSTS, category.getSlug(), category.getId())
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].title", is(post.getTitle())))
                    .andExpect(jsonPath("$.data.content[0].category.id", is(category.getId().toString())))
                    .andExpect(jsonPath("$.data.empty", is(false)))
                    .andExpect(jsonPath("$.data.first", is(true)))
                    .andExpect(jsonPath("$.data.last", is(true)))
                    .andExpect(jsonPath("$.data.numberOfElements", is(1)));
        }

        @Test
        @DisplayName("Should return 404 for a non-existent category")
        @WithMockBlogUser(USERNAME)
        void shouldReturn404ForNonExistentCategory() throws Exception {
            mockMvc.perform(get(ApiRoutes.CATEGORY_BASE_PATH + ApiRoutes.CATEGORY_POSTS, "non-existent-slug", UUID.randomUUID())
                            .param("size", "10"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.message", is(messageService.get("exception.resource.not_found", "Category"))));
        }

        @Test
        @DisplayName("Should return 200 and no posts for a category with no posts")
        @WithMockBlogUser(USERNAME)
        void shouldReturnNoPostsForValidCategory() throws Exception {
            CategoryEntity category = categoryRepository.saveAndFlush(CategoryEntity.builder()
                    .name("Test Category 1")
                    .description("This is a test category 1")
                    .build());

            mockMvc.perform(get(ApiRoutes.CATEGORY_BASE_PATH + ApiRoutes.CATEGORY_POSTS, category.getSlug(), category.getId())
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.empty", is(true)))
                    .andExpect(jsonPath("$.data.first", is(true)))
                    .andExpect(jsonPath("$.data.last", is(true)))
                    .andExpect(jsonPath("$.data.numberOfElements", is(0)))
                    .andExpect(jsonPath("$.data.content", hasSize(0)));
        }

        @Test
        @DisplayName("Should return 404 when the slug and id do not belong to the same category")
        @WithMockBlogUser(USERNAME)
        void  shouldReturn404WhenTheSlugAndIdBelongToTheSameCategory() throws Exception {
            mockMvc.perform(get(ApiRoutes.CATEGORY_BASE_PATH + ApiRoutes.CATEGORY_POSTS, "non-existent-slug", category.getId())
                            .param("size", "10"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.message", is(messageService.get("exception.resource.not_found", "Category"))));
        }

        @Test
        @DisplayName("Should return 200 and paginated slice of post when post cursor is used")
        @WithMockBlogUser(USERNAME)
        void shouldReturnPaginatedSlicesOfPostWhenPostCursorIsUsed() throws Exception {
            PostEntity post2 = postRepository.saveAndFlush(testDataFactory.createCustomPost(user, category));
            PostEntity post3 = postRepository.saveAndFlush(testDataFactory.createCustomPost(user, category));

            mockMvc.perform(get(ApiRoutes.CATEGORY_BASE_PATH + ApiRoutes.CATEGORY_POSTS, category.getSlug(), category.getId())
                            .param("size", "1")
                            .param("post_cursor", post3.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].id", is(post2.getId().toString())))
                    .andExpect(jsonPath("$.data.content[0].category.id", is(category.getId().toString())))
                    .andExpect(jsonPath("$.data.empty", is(false)))
                    .andExpect(jsonPath("$.data.first", is(true)))
                    .andExpect(jsonPath("$.data.last", is(false)))
                    .andExpect(jsonPath("$.data.numberOfElements", is(1)));
        }

        @Test
        @DisplayName("Should return 401 for unauthenticated user")
        void shouldReturn401ForUnauthenticatedUser() throws Exception {
            mockMvc.perform(get(ApiRoutes.CATEGORY_BASE_PATH + ApiRoutes.CATEGORY_POSTS, category.getSlug(), category.getId())
                            .param("size", "10"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.message", is("Full authentication is required to access this resource")));
        }
    }

    @Nested
    @DisplayName("GET " + ApiRoutes.CATEGORY_BASE_PATH)
    class GetAllCategories {

        @Test
        @DisplayName("Should return 200 and all categories")
        @WithMockBlogUser(USERNAME)
        void shouldReturnAllCategories() throws Exception {
            mockMvc.perform(get(ApiRoutes.CATEGORY_BASE_PATH))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[1].id", is(category.getId().toString())))
                    .andExpect(jsonPath("$.data[1].name", is(category.getName())))
                    .andExpect(jsonPath("$.data[1].slug", is(category.getSlug())))
                    .andExpect(jsonPath("$.data[1].description", is(category.getDescription())));
        }

        @Test
        @DisplayName("Should return 401 for unauthenticated user")
        void shouldReturn401ForUnauthenticatedUser() throws Exception {
            mockMvc.perform(get(ApiRoutes.CATEGORY_BASE_PATH))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.message", is("Full authentication is required to access this resource")));
        }
    }

    @Nested
    @DisplayName("GET " + ApiRoutes.CATEGORY_BASE_PATH + ApiRoutes.CATEGORY_PATH_VARIABLE)
    class GetCategoryDetails {

        @Test
        @DisplayName("Should return 200 and category details for a valid category")
        @WithMockBlogUser(USERNAME)
        void shouldReturnCategoryDetailsForValidCategory() throws Exception {
            mockMvc.perform(get(ApiRoutes.CATEGORY_BASE_PATH + ApiRoutes.CATEGORY_PATH_VARIABLE, category.getSlug(), category.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id", is(category.getId().toString())))
                    .andExpect(jsonPath("$.data.name", is(category.getName())))
                    .andExpect(jsonPath("$.data.slug", is(category.getSlug())))
                    .andExpect(jsonPath("$.data.description", is(category.getDescription())));
        }

        @Test
        @DisplayName("Should return 404 for a non-existent category")
        @WithMockBlogUser(USERNAME)
        void shouldReturn404ForNonExistentCategory() throws Exception {
            mockMvc.perform(get(ApiRoutes.CATEGORY_BASE_PATH + ApiRoutes.CATEGORY_PATH_VARIABLE, "non-existent-slug", UUID.randomUUID()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.message", is(messageService.get("exception.resource.not_found", "Category"))));
        }

        @Test
        @DisplayName("Should return 404 when the slug and id do not belong to the same category")
        @WithMockBlogUser(USERNAME)
        void shouldReturn404WhenTheSlugAndIdBelongToTheSameCategory() throws Exception {
            mockMvc.perform(get(ApiRoutes.CATEGORY_BASE_PATH + ApiRoutes.CATEGORY_PATH_VARIABLE, "non-existent-slug", category.getId()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.message", is(messageService.get("exception.resource.not_found", "Category"))));
        }

        @Test
        @DisplayName("Should return 401 for unauthenticated user")
        void shouldReturn401ForUnauthenticatedUser() throws Exception {
            mockMvc.perform(get(ApiRoutes.CATEGORY_BASE_PATH + ApiRoutes.CATEGORY_PATH_VARIABLE, category.getSlug(), category.getId()))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.message", is("Full authentication is required to access this resource")));
        }
    }
}
