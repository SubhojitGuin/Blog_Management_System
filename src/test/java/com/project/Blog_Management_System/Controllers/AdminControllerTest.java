package com.project.Blog_Management_System.Controllers;

import com.project.Blog_Management_System.Constants.ApiRoutes;
import com.project.Blog_Management_System.Dto.CategoryRequestDTO;
import com.project.Blog_Management_System.Dto.CategoryResponseDTO;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@Import(WebSecurityConfig.class)
public class AdminControllerTest {

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

    @Autowired
    private ObjectMapper objectMapper;

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
    @DisplayName("createCategory(CategoryRequestDTO)")
    class CreateCategory {

        @Test
        @DisplayName("returns 201 Created when the category is created successfully")
        @WithMockUser(username = "test-user", roles = {"ADMIN"})
        void createsCategorySuccessfully() throws Exception {
            CategoryRequestDTO requestDTO = new CategoryRequestDTO();
            requestDTO.setName(category.getName());
            requestDTO.setDescription(category.getDescription());

            when(categoryService.createCategory(any(CategoryRequestDTO.class))).thenReturn(categoryResponseDTO);

            mockMvc.perform(post(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.CATEGORY_BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.id").value(categoryResponseDTO.getId().toString()))
                    .andExpect(jsonPath("$.data.name").value(categoryResponseDTO.getName()))
                    .andExpect(jsonPath("$.data.description").value(categoryResponseDTO.getDescription()))
                    .andExpect(jsonPath("$.data.slug").value(categoryResponseDTO.getSlug()));
        }

        @Test
        @DisplayName("denies access when user is not an admin")
        @WithMockUser(username = "test-user", roles = {"USER"})
        void deniesAccessWhenUserIsNotAdmin() throws Exception {
            CategoryRequestDTO requestDTO = new CategoryRequestDTO();
            requestDTO.setName(category.getName());
            requestDTO.setDescription(category.getDescription());

            mockMvc.perform(post(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.CATEGORY_BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("requires authentication and returns 401 when unauthenticated")
        void requiresAuthenticationWhenUnauthenticated() throws Exception {
            CategoryRequestDTO requestDTO = new CategoryRequestDTO();
            requestDTO.setName(category.getName());
            requestDTO.setDescription(category.getDescription());

            mockMvc.perform(post(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.CATEGORY_BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("updateCategory(CategoryRequestDTO, String, UUID)")
    class UpdateCategory {

        @Test
        @DisplayName("returns 200 OK when category is updated successfully")
        @WithMockUser(username = "test-user", roles = {"ADMIN"})
        void updatesCategorySuccessfully() throws Exception {
            CategoryRequestDTO requestDTO = new CategoryRequestDTO();
            requestDTO.setName("Updated Category");
            requestDTO.setDescription("Updated Description");

            categoryResponseDTO.setName("Updated Category");
            categoryResponseDTO.setDescription("Updated Description");

            when(categoryService.updateCategory(eq(category_slug), eq(category_id), any(CategoryRequestDTO.class)))
                    .thenReturn(categoryResponseDTO);

            mockMvc.perform(put(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.ADMIN_CATEGORY_PATH, category_slug, category_id.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(categoryResponseDTO.getId().toString()))
                    .andExpect(jsonPath("$.data.name").value(categoryResponseDTO.getName()))
                    .andExpect(jsonPath("$.data.description").value(categoryResponseDTO.getDescription()));
        }

        @Test
        @DisplayName("denies access when user is not an admin")
        @WithMockUser(username = "test-user", roles = {"USER"})
        void deniesAccessWhenUserIsNotAdmin() throws Exception {
            CategoryRequestDTO requestDTO = new CategoryRequestDTO();
            requestDTO.setName("Updated Category");
            requestDTO.setDescription("Updated Description");

            mockMvc.perform(put(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.ADMIN_CATEGORY_PATH, category_slug, category_id.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("requires authentication and returns 401 when unauthenticated")
        void requiresAuthenticationWhenUnauthenticated() throws Exception {
            CategoryRequestDTO requestDTO = new CategoryRequestDTO();
            requestDTO.setName("Updated Category");
            requestDTO.setDescription("Updated Description");

            mockMvc.perform(put(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.ADMIN_CATEGORY_PATH, category_slug, category_id.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("deleteCategory(String, UUID, String)")
    class DeleteCategory {

        @Test
        @DisplayName("returns 204 No Content when category is deleted successfully")
        @WithMockUser(username = "test-user", roles = {"ADMIN"})
        void deletesCategorySuccessfully() throws Exception {
            doNothing().when(categoryService).deleteCategory(eq(category_slug), eq(category_id), eq("uncategorised"));

            mockMvc.perform(delete(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.ADMIN_CATEGORY_PATH, category_slug, category_id.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("uses provided newSlug parameter when specified")
        @WithMockUser(username = "test-user", roles = {"ADMIN"})
        void usesProvidedNewSlugWhenSpecified() throws Exception {
            String newSlug = "misc";
            doNothing().when(categoryService).deleteCategory(eq(category_slug), eq(category_id), eq(newSlug));

            mockMvc.perform(delete(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.ADMIN_CATEGORY_PATH, category_slug, category_id.toString())
                            .param("newSlug", newSlug))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("denies access when user is not an admin")
        @WithMockUser(username = "test-user", roles = {"USER"})
        void deniesAccessWhenUserIsNotAdmin() throws Exception {
            mockMvc.perform(delete(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.ADMIN_CATEGORY_PATH, category_slug, category_id.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("requires authentication and returns 401 when unauthenticated")
        void requiresAuthenticationWhenUnauthenticated() throws Exception {
            mockMvc.perform(delete(ApiRoutes.ADMIN_BASE_PATH + ApiRoutes.ADMIN_CATEGORY_PATH, category_slug, category_id.toString()))
                    .andExpect(status().isUnauthorized());
        }
    }
}
