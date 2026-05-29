package com.project.Blog_Management_System.Controllers;

import com.project.Blog_Management_System.Constants.ApiRoutes;
import com.project.Blog_Management_System.Dto.CategoryResponseDTO;
import com.project.Blog_Management_System.Dto.PostResponseDTO;
import com.project.Blog_Management_System.Entities.CategoryEntity;
import com.project.Blog_Management_System.Security.JWTService;
import com.project.Blog_Management_System.Security.WebSecurityConfig;
import com.project.Blog_Management_System.Service.Interfaces.CategoryService;
import com.project.Blog_Management_System.Service.Interfaces.UserService;
import com.project.Blog_Management_System.Utils.MessageService;
import com.project.Blog_Management_System.Utils.TestEntityFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@Import(WebSecurityConfig.class)
public class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private MessageService messageService;

    @MockitoBean
    private JWTService jwtService;

    @MockitoBean
    private UserService userService;

    private CategoryEntity category;
    private CategoryResponseDTO categoryResponseDTO;
    private String category_slug;
    private UUID category_id;

    @BeforeEach
    void setUp() {
        category = TestEntityFactory.testCategory("main");
        category.setId(UUID.randomUUID());

        categoryResponseDTO = new CategoryResponseDTO();
        categoryResponseDTO.setId(category.getId());
        categoryResponseDTO.setName(category.getName());
        categoryResponseDTO.setDescription(category.getDescription());
        categoryResponseDTO.setSlug(category.getSlug());

        category_slug = category.getSlug();
        category_id = category.getId();
    }

    @Nested
    @DisplayName("getPostsByCategory(String, UUID, UUID, Integer)")
    class GetPostsByCategory {

        @Test
        @DisplayName("returns 200 OK with paginated posts for valid category")
        @WithMockUser(username = "test-user", roles = {"USER"})
        void returnsPaginatedPostsForValidCategory() throws Exception {
            PostResponseDTO postDTO = new PostResponseDTO();
            postDTO.setId(UUID.randomUUID());
            postDTO.setTitle("Test Post");

            Slice<PostResponseDTO> postsSlice = new SliceImpl<>(List.of(postDTO));

            when(categoryService.getPostsByCategory(eq(category_slug), eq(category_id), any(), eq(10)))
                    .thenReturn(postsSlice);

            mockMvc.perform(get(ApiRoutes.CATEGORY_BASE_PATH + ApiRoutes.CATEGORY_POSTS, category_slug, category_id.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].id").value(postDTO.getId().toString()))
                    .andExpect(jsonPath("$.data.content[0].title").value(postDTO.getTitle()));
        }

        @Test
        @DisplayName("returns 200 OK with default size when size parameter not provided")
        @WithMockUser(username = "test-user", roles = {"USER"})
        void returnsPostsWithDefaultSizeWhenNotProvided() throws Exception {
            Slice<PostResponseDTO> postsSlice = new SliceImpl<>(Collections.emptyList());

            when(categoryService.getPostsByCategory(eq(category_slug), eq(category_id), any(), eq(10)))
                    .thenReturn(postsSlice);

            mockMvc.perform(get(ApiRoutes.CATEGORY_BASE_PATH + ApiRoutes.CATEGORY_POSTS, category_slug, category_id.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("returns 200 OK with custom size parameter when provided")
        @WithMockUser(username = "test-user", roles = {"USER"})
        void returnsPostsWithCustomSizeWhenProvided() throws Exception {
            Slice<PostResponseDTO> postsSlice = new SliceImpl<>(Collections.emptyList());

            when(categoryService.getPostsByCategory(eq(category_slug), eq(category_id), any(), eq(20)))
                    .thenReturn(postsSlice);

            mockMvc.perform(get(ApiRoutes.CATEGORY_BASE_PATH + ApiRoutes.CATEGORY_POSTS, category_slug, category_id.toString())
                            .param("size", "20")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("returns 200 OK with cursor parameter for pagination")
        @WithMockUser(username = "test-user", roles = {"USER"})
        void returnsPaginatedPostsUsingCursorParameter() throws Exception {
            UUID cursorId = UUID.randomUUID();
            Slice<PostResponseDTO> postsSlice = new SliceImpl<>(Collections.emptyList());

            when(categoryService.getPostsByCategory(eq(category_slug), eq(category_id), eq(cursorId), eq(10)))
                    .thenReturn(postsSlice);

            mockMvc.perform(get(ApiRoutes.CATEGORY_BASE_PATH + ApiRoutes.CATEGORY_POSTS, category_slug, category_id.toString())
                            .param("post_cursor", cursorId.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("returns 200 OK with empty list when category has no posts")
        @WithMockUser(username = "test-user", roles = {"USER"})
        void returnsEmptyListWhenCategoryHasNoPosts() throws Exception {
            Slice<PostResponseDTO> emptySlice = new SliceImpl<>(Collections.emptyList());

            when(categoryService.getPostsByCategory(eq(category_slug), eq(category_id), any(), eq(10)))
                    .thenReturn(emptySlice);

            mockMvc.perform(get(ApiRoutes.CATEGORY_BASE_PATH + ApiRoutes.CATEGORY_POSTS, category_slug, category_id.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("requires authentication and returns 401 when unauthenticated")
        void requiresAuthenticationWhenUnauthenticated() throws Exception {
            mockMvc.perform(get(ApiRoutes.CATEGORY_BASE_PATH + ApiRoutes.CATEGORY_POSTS, category_slug, category_id.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("getAllCategories()")
    class GetAllCategories {

        @Test
        @DisplayName("returns 200 OK with list of all categories")
        @WithMockUser(username = "test-user", roles = {"USER"})
        void returnsAllCategoriesSuccessfully() throws Exception {
            when(categoryService.getAllCategories()).thenReturn(List.of(categoryResponseDTO));

            mockMvc.perform(get(ApiRoutes.CATEGORY_BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].id").value(categoryResponseDTO.getId().toString()))
                    .andExpect(jsonPath("$.data[0].name").value(categoryResponseDTO.getName()))
                    .andExpect(jsonPath("$.data[0].description").value(categoryResponseDTO.getDescription()))
                    .andExpect(jsonPath("$.data[0].slug").value(categoryResponseDTO.getSlug()));
        }

        @Test
        @DisplayName("returns 200 OK with empty list when no categories exist")
        @WithMockUser(username = "test-user", roles = {"USER"})
        void returnsEmptyListWhenNoCategoriesExist() throws Exception {
            when(categoryService.getAllCategories()).thenReturn(Collections.emptyList());

            mockMvc.perform(get(ApiRoutes.CATEGORY_BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("returns 200 OK with multiple categories when they exist")
        @WithMockUser(username = "test-user", roles = {"USER"})
        void returnsMultipleCategoriesWhenTheyExist() throws Exception {
            CategoryResponseDTO category2 = new CategoryResponseDTO();
            category2.setId(UUID.randomUUID());
            category2.setName("Sports");
            category2.setDescription("Sports news");
            category2.setSlug("sports");

            when(categoryService.getAllCategories()).thenReturn(List.of(categoryResponseDTO, category2));

            mockMvc.perform(get(ApiRoutes.CATEGORY_BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].slug").value(categoryResponseDTO.getSlug()))
                    .andExpect(jsonPath("$.data[1].slug").value(category2.getSlug()));
        }

        @Test
        @DisplayName("requires authentication and returns 401 when unauthenticated")
        void requiresAuthenticationWhenUnauthenticated() throws Exception {
            mockMvc.perform(get(ApiRoutes.CATEGORY_BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("getCategoryDetails(String, UUID)")
    class GetCategoryDetails {

        @Test
        @DisplayName("returns 200 OK with category details for valid slug and id")
        @WithMockUser(username = "test-user", roles = {"USER"})
        void returnsCategoryDetailsSuccessfully() throws Exception {
            when(categoryService.getCategoryDetails(eq(category_slug), eq(category_id)))
                    .thenReturn(categoryResponseDTO);

            mockMvc.perform(get(ApiRoutes.CATEGORY_BASE_PATH + ApiRoutes.CATEGORY_PATH_VARIABLE, category_slug, category_id.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(categoryResponseDTO.getId().toString()))
                    .andExpect(jsonPath("$.data.name").value(categoryResponseDTO.getName()))
                    .andExpect(jsonPath("$.data.description").value(categoryResponseDTO.getDescription()))
                    .andExpect(jsonPath("$.data.slug").value(categoryResponseDTO.getSlug()));
        }

        @Test
        @DisplayName("passes correct slug and id path variables to service")
        @WithMockUser(username = "test-user", roles = {"USER"})
        void passesCorrectPathVariablesToService() throws Exception {
            String testSlug = "tech-news";
            UUID testId = UUID.randomUUID();

            when(categoryService.getCategoryDetails(eq(testSlug), eq(testId)))
                    .thenReturn(categoryResponseDTO);

            mockMvc.perform(get(ApiRoutes.CATEGORY_BASE_PATH + ApiRoutes.CATEGORY_PATH_VARIABLE, testSlug, testId.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("requires authentication and returns 401 when unauthenticated")
        void requiresAuthenticationWhenUnauthenticated() throws Exception {
            mockMvc.perform(get(ApiRoutes.CATEGORY_BASE_PATH + ApiRoutes.CATEGORY_PATH_VARIABLE, category_slug, category_id.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }
    }
}

