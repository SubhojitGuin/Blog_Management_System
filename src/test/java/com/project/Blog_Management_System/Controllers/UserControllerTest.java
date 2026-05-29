package com.project.Blog_Management_System.Controllers;

import com.project.Blog_Management_System.Constants.ApiRoutes;
import com.project.Blog_Management_System.Dto.*;
import com.project.Blog_Management_System.Enums.Gender;
import com.project.Blog_Management_System.Exceptions.InvalidActionException;
import com.project.Blog_Management_System.Exceptions.ResourceConflictException;
import com.project.Blog_Management_System.Exceptions.ResourceNotFoundException;
import com.project.Blog_Management_System.Security.JWTService;
import com.project.Blog_Management_System.Security.WebSecurityConfig;
import com.project.Blog_Management_System.Service.Interfaces.UserService;
import com.project.Blog_Management_System.Utils.MessageService;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(WebSecurityConfig.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private MessageService messageService;

    @MockitoBean
    private JWTService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID userId;
    private String username;
    private UserDTO userDTO;
    private UserInfoDTO userInfoDTO;
    private ProfileUpdateDTO profileUpdateDTO;
    private PasswordUpdateDTO passwordUpdateDTO;
    private UsernameUpdateDTO usernameUpdateDTO;
    private EmailUpdateDTO emailUpdateDTO;
    private FollowDTO followDTO;
    private FollowInfoDTO followInfoDTO;
    private PostInfoDTO postInfoDTO;
    private BookmarkInfoDTO bookmarkInfoDTO;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        username = "testuser";

        userDTO = new UserDTO();
        userDTO.setId(userId);
        userDTO.setName("Test User");
        userDTO.setUsername(username);
        userDTO.setBio("Bio");
        userDTO.setNoOfFollowers(3);
        userDTO.setNoOfFollowings(5);
        userDTO.setNoOfPosts(2);
        userDTO.setIsDeleted(false);
        userDTO.setIsCurrentUser(true);

        userInfoDTO = new UserInfoDTO(userId, userDTO.getName(), username, true);

        profileUpdateDTO = new ProfileUpdateDTO();
        profileUpdateDTO.setName("Updated Name");
        profileUpdateDTO.setBio("Updated bio");
        profileUpdateDTO.setGender(Gender.MALE);
        profileUpdateDTO.setDateOfBirth(LocalDate.of(2000, 1, 1));

        passwordUpdateDTO = new PasswordUpdateDTO();
        passwordUpdateDTO.setOldPassword("Password@123");
        passwordUpdateDTO.setNewPassword("Password@456");

        usernameUpdateDTO = new UsernameUpdateDTO();
        usernameUpdateDTO.setUsername("updateduser");

        emailUpdateDTO = new EmailUpdateDTO();
        emailUpdateDTO.setEmail("updated@example.com");

        followDTO = new FollowDTO();
        followDTO.setFollow(true);

        followInfoDTO = new FollowInfoDTO(userInfoDTO, UUID.randomUUID());

        postInfoDTO = new PostInfoDTO(UUID.randomUUID(), "sample-post", "Sample Post", "Sample description", 3, 10, 2, 100L);
        bookmarkInfoDTO = new BookmarkInfoDTO(UUID.randomUUID(), postInfoDTO, LocalDate.now().atStartOfDay());
    }

    @Nested
    @DisplayName("updateUserProfile(ProfileUpdateDTO)")
    class UpdateUserProfile {

        @Test
        @DisplayName("returns 200 OK when profile is updated successfully")
        @WithMockUser(username = "reader")
        void updatesProfileSuccessfully() throws Exception {
            when(userService.updateProfile(any(ProfileUpdateDTO.class))).thenReturn(profileUpdateDTO);

            mockMvc.perform(put(ApiRoutes.USERS_BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(profileUpdateDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value(profileUpdateDTO.getName()))
                    .andExpect(jsonPath("$.data.bio").value(profileUpdateDTO.getBio()))
                    .andExpect(jsonPath("$.data.gender").value(profileUpdateDTO.getGender().name()));
        }

        @Test
        @DisplayName("returns 400 Bad Request when profile name is blank")
        @WithMockUser(username = "reader")
        void returnsBadRequestWhenProfileNameIsBlank() throws Exception {
            profileUpdateDTO.setName("");

            mockMvc.perform(put(ApiRoutes.USERS_BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(profileUpdateDTO)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("updateUserPassword(PasswordUpdateDTO, HttpServletResponse)")
    class UpdateUserPassword {

        @Test
        @DisplayName("returns 204 No Content when password is updated successfully")
        @WithMockUser(username = "reader")
        void updatesPasswordSuccessfully() throws Exception {
            doNothing().when(userService).updatePassword(any(PasswordUpdateDTO.class));

            mockMvc.perform(patch(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_UPDATE_PASSWORD_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(passwordUpdateDTO)))
                    .andExpect(status().isNoContent())
                    .andExpect(cookie().exists("refreshToken"));
        }

        @Test
        @DisplayName("returns 401 Unauthorized when old password is invalid")
        @WithMockUser(username = "reader")
        void returnsUnauthorizedWhenOldPasswordIsInvalid() throws Exception {
            doThrow(new BadCredentialsException("Invalid old password"))
                    .when(userService).updatePassword(any(PasswordUpdateDTO.class));

            mockMvc.perform(patch(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_UPDATE_PASSWORD_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(passwordUpdateDTO)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("returns 400 Bad Request when new password is missing")
        @WithMockUser(username = "reader")
        void returnsBadRequestWhenNewPasswordIsMissing() throws Exception {
            passwordUpdateDTO.setNewPassword(null);

            mockMvc.perform(patch(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_UPDATE_PASSWORD_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(passwordUpdateDTO)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("updateUserName(UsernameUpdateDTO, HttpServletResponse)")
    class UpdateUserName {

        @Test
        @DisplayName("returns 204 No Content when username is updated successfully")
        @WithMockUser(username = "reader")
        void updatesUsernameSuccessfully() throws Exception {
            doNothing().when(userService).updateUserName(any(UsernameUpdateDTO.class));

            mockMvc.perform(patch(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_UPDATE_USERNAME_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(usernameUpdateDTO)))
                    .andExpect(status().isNoContent())
                    .andExpect(cookie().exists("refreshToken"));
        }

        @Test
        @DisplayName("returns 409 Conflict when username already exists")
        @WithMockUser(username = "reader")
        void returnsConflictWhenUsernameAlreadyExists() throws Exception {
            doThrow(new ResourceConflictException("Username already exists"))
                    .when(userService).updateUserName(any(UsernameUpdateDTO.class));

            mockMvc.perform(patch(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_UPDATE_USERNAME_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(usernameUpdateDTO)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("returns 400 Bad Request when username is blank")
        @WithMockUser(username = "reader")
        void returnsBadRequestWhenUsernameIsBlank() throws Exception {
            usernameUpdateDTO.setUsername("");

            mockMvc.perform(patch(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_UPDATE_USERNAME_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(usernameUpdateDTO)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("updateEmail(EmailUpdateDTO, HttpServletResponse)")
    class UpdateEmail {

        @Test
        @DisplayName("returns 204 No Content when email is updated successfully")
        @WithMockUser(username = "reader")
        void updatesEmailSuccessfully() throws Exception {
            doNothing().when(userService).updateEmail(any(EmailUpdateDTO.class));

            mockMvc.perform(patch(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_UPDATE_EMAIL_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(emailUpdateDTO)))
                    .andExpect(status().isNoContent())
                    .andExpect(cookie().exists("refreshToken"));
        }

        @Test
        @DisplayName("returns 409 Conflict when email already exists")
        @WithMockUser(username = "reader")
        void returnsConflictWhenEmailAlreadyExists() throws Exception {
            doThrow(new ResourceConflictException("Email already exists"))
                    .when(userService).updateEmail(any(EmailUpdateDTO.class));

            mockMvc.perform(patch(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_UPDATE_EMAIL_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(emailUpdateDTO)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("returns 400 Bad Request when email is invalid")
        @WithMockUser(username = "reader")
        void returnsBadRequestWhenEmailIsInvalid() throws Exception {
            emailUpdateDTO.setEmail("invalid-email");

            mockMvc.perform(patch(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_UPDATE_EMAIL_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(emailUpdateDTO)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("getUserProfile(String, UUID)")
    class GetUserProfile {

        @Test
        @DisplayName("returns 200 OK with user profile for valid username and id")
        @WithMockUser(username = "reader")
        void returnsUserProfileSuccessfully() throws Exception {
            when(userService.getUserProfile(eq(username), eq(userId))).thenReturn(userDTO);

            mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_PATH_VARIABLE, username, userId.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(userDTO.getId().toString()))
                    .andExpect(jsonPath("$.data.name").value(userDTO.getName()))
                    .andExpect(jsonPath("$.data.username").value(userDTO.getUsername()));
        }

        @Test
        @DisplayName("returns 404 Not Found when user does not exist")
        @WithMockUser(username = "reader")
        void returnsNotFoundWhenUserDoesNotExist() throws Exception {
            when(userService.getUserProfile(eq(username), eq(userId))).thenThrow(new ResourceNotFoundException("User not found"));

            mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_PATH_VARIABLE, username, userId.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("searchUsers(String)")
    class SearchUsers {

        @Test
        @DisplayName("returns 200 OK with matching users for a valid query")
        @WithMockUser(username = "reader")
        void returnsMatchingUsersSuccessfully() throws Exception {
            when(userService.searchUsers(eq("test"))).thenReturn(List.of(userInfoDTO));

            mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_SEARCH_PATH)
                            .param("query", "test")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].id").value(userInfoDTO.getId().toString()))
                    .andExpect(jsonPath("$.data[0].username").value(userInfoDTO.getUsername()));
        }

        @Test
        @DisplayName("returns 200 OK with empty result when no users match")
        @WithMockUser(username = "reader")
        void returnsEmptyResultWhenNoUsersMatch() throws Exception {
            when(userService.searchUsers(eq("nomatch"))).thenReturn(Collections.emptyList());

            mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_SEARCH_PATH)
                            .param("query", "nomatch")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("followOrUnfollowUser(String, UUID, FollowDTO)")
    class FollowOrUnfollowUser {

        @Test
        @DisplayName("returns 204 No Content when a user is followed successfully")
        @WithMockUser(username = "reader")
        void followsUserSuccessfully() throws Exception {
            doNothing().when(userService).followOrUnfollowUser(eq(username), eq(userId), any(FollowDTO.class));

            mockMvc.perform(post(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_FOLLOW_PATH, username, userId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(followDTO)))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("returns 406 Not Acceptable when attempting to follow oneself")
        @WithMockUser(username = "reader")
        void returnsNotAcceptableWhenFollowingSelf() throws Exception {
            doThrow(new InvalidActionException("Cannot follow yourself"))
                    .when(userService).followOrUnfollowUser(eq(username), eq(userId), any(FollowDTO.class));

            mockMvc.perform(post(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_FOLLOW_PATH, username, userId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(followDTO)))
                    .andExpect(status().isNotAcceptable());
        }

        @Test
        @DisplayName("returns 400 Bad Request when follow flag is missing")
        @WithMockUser(username = "reader")
        void returnsBadRequestWhenFollowFlagIsMissing() throws Exception {
            followDTO.setFollow(null);

            mockMvc.perform(post(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_FOLLOW_PATH, username, userId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(followDTO)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("getFollowers(String, UUID, UUID, int)")
    class GetFollowers {

        @Test
        @DisplayName("returns 200 OK with followers for a valid user")
        @WithMockUser(username = "reader")
        void returnsFollowersSuccessfully() throws Exception {
            Slice<FollowInfoDTO> slice = new SliceImpl<>(List.of(followInfoDTO));

            when(userService.getFollowers(eq(username), eq(userId), any(), eq(10))).thenReturn(slice);

            mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_FOLLOWERS_PATH, username, userId.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].followId").value(followInfoDTO.getFollowId().toString()))
                    .andExpect(jsonPath("$.data.content[0].user.username").value(userInfoDTO.getUsername()));
        }

        @Test
        @DisplayName("returns 200 OK with followers when cursor and custom size are provided")
        @WithMockUser(username = "reader")
        void returnsFollowersUsingCursorAndCustomSize() throws Exception {
            UUID cursor = UUID.randomUUID();
            Slice<FollowInfoDTO> slice = new SliceImpl<>(Collections.emptyList());

            when(userService.getFollowers(eq(username), eq(userId), eq(cursor), eq(25))).thenReturn(slice);

            mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_FOLLOWERS_PATH, username, userId.toString())
                            .param("follow_cursor", cursor.toString())
                            .param("size", "25")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("getFollowings(String, UUID, UUID, int)")
    class GetFollowings {

        @Test
        @DisplayName("returns 200 OK with followings for a valid user")
        @WithMockUser(username = "reader")
        void returnsFollowingsSuccessfully() throws Exception {
            Slice<FollowInfoDTO> slice = new SliceImpl<>(List.of(followInfoDTO));

            when(userService.getFollowings(eq(username), eq(userId), any(), eq(10))).thenReturn(slice);

            mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_FOLLOWINGS_PATH, username, userId.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].followId").value(followInfoDTO.getFollowId().toString()))
                    .andExpect(jsonPath("$.data.content[0].user.username").value(userInfoDTO.getUsername()));
        }

        @Test
        @DisplayName("returns 200 OK with followings when cursor and custom size are provided")
        @WithMockUser(username = "reader")
        void returnsFollowingsUsingCursorAndCustomSize() throws Exception {
            UUID cursor = UUID.randomUUID();
            Slice<FollowInfoDTO> slice = new SliceImpl<>(Collections.emptyList());

            when(userService.getFollowings(eq(username), eq(userId), eq(cursor), eq(5))).thenReturn(slice);

            mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_FOLLOWINGS_PATH, username, userId.toString())
                            .param("follow_cursor", cursor.toString())
                            .param("size", "5")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("deleteUser()")
    class DeleteUser {

        @Test
        @DisplayName("returns 204 No Content when user account is deleted successfully")
        @WithMockUser(username = "reader")
        void deletesUserSuccessfully() throws Exception {
            doNothing().when(userService).deleteUser();

            mockMvc.perform(delete(ApiRoutes.USERS_BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("getUserPosts(String, UUID, UUID, int)")
    class GetUserPosts {

        @Test
        @DisplayName("returns 200 OK with posts for a valid user")
        @WithMockUser(username = "reader")
        void returnsUserPostsSuccessfully() throws Exception {
            Slice<PostInfoDTO> slice = new SliceImpl<>(List.of(postInfoDTO));

            when(userService.getUserPosts(eq(username), eq(userId), any(), eq(10))).thenReturn(slice);

            mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_POSTS_PATH, username, userId.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].id").value(postInfoDTO.getId().toString()))
                    .andExpect(jsonPath("$.data.content[0].slug").value(postInfoDTO.getSlug()));
        }

        @Test
        @DisplayName("returns 200 OK with posts when cursor and custom size are provided")
        @WithMockUser(username = "reader")
        void returnsUserPostsUsingCursorAndCustomSize() throws Exception {
            UUID cursor = UUID.randomUUID();
            Slice<PostInfoDTO> slice = new SliceImpl<>(Collections.emptyList());

            when(userService.getUserPosts(eq(username), eq(userId), eq(cursor), eq(25))).thenReturn(slice);

            mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_POSTS_PATH, username, userId.toString())
                            .param("post_cursor", cursor.toString())
                            .param("size", "25")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("returns 404 Not Found when user does not exist")
        @WithMockUser(username = "reader")
        void returnsNotFoundWhenUserPostsMissingUser() throws Exception {
            when(userService.getUserPosts(eq(username), eq(userId), any(), eq(10)))
                    .thenThrow(new ResourceNotFoundException("User not found"));

            mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_POSTS_PATH, username, userId.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("getUserBookmarks(UUID, int)")
    class GetUserBookmarks {

        @Test
        @DisplayName("returns 200 OK with bookmarks for current user")
        @WithMockUser(username = "reader")
        void returnsUserBookmarksSuccessfully() throws Exception {
            Slice<BookmarkInfoDTO> slice = new SliceImpl<>(List.of(bookmarkInfoDTO));

            when(userService.getUserBookmarks(any(), eq(10))).thenReturn(slice);

            mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_BOOKMARKS_PATH)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].id").value(bookmarkInfoDTO.getId().toString()))
                    .andExpect(jsonPath("$.data.content[0].post.slug").value(postInfoDTO.getSlug()));
        }

        @Test
        @DisplayName("returns 200 OK with bookmarks when cursor and custom size are provided")
        @WithMockUser(username = "reader")
        void returnsUserBookmarksUsingCursorAndCustomSize() throws Exception {
            UUID cursor = UUID.randomUUID();
            Slice<BookmarkInfoDTO> slice = new SliceImpl<>(Collections.emptyList());

            when(userService.getUserBookmarks(eq(cursor), eq(25))).thenReturn(slice);

            mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_BOOKMARKS_PATH)
                            .param("bookmark_cursor", cursor.toString())
                            .param("size", "25")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }
}

