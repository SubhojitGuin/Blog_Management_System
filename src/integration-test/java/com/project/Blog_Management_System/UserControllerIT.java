package com.project.Blog_Management_System;

import com.project.Blog_Management_System.Advice.ApiError;
import com.project.Blog_Management_System.Annotations.WithMockBlogUser;
import com.project.Blog_Management_System.Constants.ApiRoutes;
import com.project.Blog_Management_System.Dto.*;
import com.project.Blog_Management_System.Entities.*;
import com.project.Blog_Management_System.Repositories.*;
import com.project.Blog_Management_System.Utils.TestSliceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UserControllerIT extends BaseIT {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String USERNAME = "testuser";
    private static final String PASSWORD = "TestPassword@123";
    private UserEntity user;

    @BeforeEach
    void setup() {
        user = userRepository.saveAndFlush(testDataFactory.createCustomUser(USERNAME, "testuser@gmail.com", PASSWORD));
    }

    @Nested
    @DisplayName("PUT " + ApiRoutes.USERS_BASE_PATH)
    class UpdateUserProfile {

        @Test
        @DisplayName("Should return 200, and update user profile")
        @WithMockBlogUser(USERNAME)
        void shouldReturn200AndUpdateUserProfile() throws Exception {
            ProfileUpdateDTO profileUpdateRequest = ProfileUpdateDTO.builder()
                    .name("Updated User Name")
                    .bio(user.getBio())
                    .gender(user.getGender())
                    .dateOfBirth(user.getDateOfBirth())
                    .build();

            MvcResult response = mockMvc.perform(put(ApiRoutes.USERS_BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(profileUpdateRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            ProfileUpdateDTO profileUpdateDTO = testResponseExtractor.extractPayload(response, ProfileUpdateDTO.class);
            assertThat(profileUpdateDTO.getName()).isEqualTo("Updated User Name");

            assertThat(userRepository.findById(user.getId()))
                    .isPresent()
                    .get()
                    .satisfies(user -> assertThat(user.getName()).isEqualTo(profileUpdateDTO.getName()));
        }

        @Test
        @DisplayName("Should return 400 when name and dateOfBirth are null")
        @WithMockBlogUser(USERNAME)
        void shouldReturn400WhenNameAndDateOfBirthAreNull() throws Exception {
            ProfileUpdateDTO profileUpdateRequest = ProfileUpdateDTO.builder()
                    .bio("Updated Bio")
                    .build();

            MvcResult response = mockMvc.perform(put(ApiRoutes.USERS_BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(profileUpdateRequest)))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.validation.failed"));
            assertThat(errorResponse.getSubErrors())
                    .hasSize(2)
                    .extracting(ApiError.FieldError::getField, ApiError.FieldError::getMessage)
                    .containsExactlyInAnyOrder(
                            tuple("name", messageService.get("validation.user.name.not_blank")),
                            tuple("dateOfBirth", messageService.get("validation.user.dob.not_null"))
                    );

            assertThat(userRepository.findById(user.getId())).isPresent()
                    .get()
                    .satisfies(user -> assertThat(user.getBio()).isNotEqualTo(profileUpdateRequest.getBio()));
        }


        @Test
        @DisplayName("Should return 400 when input fails validation constraints")
        @WithMockBlogUser(USERNAME)
        void shouldReturn400WhenInputFailsValidationConstraints() throws Exception {
            ProfileUpdateDTO profileUpdateDTO = ProfileUpdateDTO.builder()
                    .name("Updated User Name")
                    .bio("Updated Bio")
                    .dateOfBirth(LocalDate.now().plusYears(2))
                    .build();

            MvcResult response = mockMvc.perform(put(ApiRoutes.USERS_BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(profileUpdateDTO)))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.validation.failed"));
            assertThat(errorResponse.getSubErrors())
                    .hasSize(1)
                    .extracting(ApiError.FieldError::getField, ApiError.FieldError::getMessage)
                    .containsExactlyInAnyOrder(tuple("dateOfBirth", messageService.get("validation.user.dob")));

            assertThat(userRepository.findById(user.getId()))
                    .isPresent()
                    .get()
                    .satisfies(user -> assertThat(user.getBio()).isNotEqualTo(profileUpdateDTO.getBio()));
        }

        @Test
        @DisplayName("Should return 401 when the user is unauthenticated")
        void shouldReturn401WhenUserIsUnauthenticated() throws Exception {
            ProfileUpdateDTO profileUpdateDTO = ProfileUpdateDTO.builder()
                    .name("Updated User Name")
                    .bio(user.getBio())
                    .gender(user.getGender())
                    .dateOfBirth(user.getDateOfBirth())
                    .build();

            MvcResult response = mockMvc.perform(put(ApiRoutes.USERS_BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(profileUpdateDTO)))
                    .andExpect(status().isUnauthorized())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo("Full authentication is required to access this resource");

            assertThat(userRepository.findById(user.getId()))
                    .isPresent()
                    .get()
                    .satisfies(user -> assertThat(user.getName()).isNotEqualTo(profileUpdateDTO.getName()));
        }
    }

    @Nested
    @DisplayName("PATCH " + ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_UPDATE_PASSWORD_PATH)
    class UpdateUserPassword {

        @Test
        @DisplayName("Should return 204, and updates user password")
        @WithMockBlogUser(USERNAME)
        void shouldReturn204AndUpdateUserPassword() throws Exception {
            PasswordUpdateDTO passwordUpdateDTO = PasswordUpdateDTO.builder()
                    .oldPassword(PASSWORD)
                    .newPassword("NewPassword@123")
                    .build();

            mockMvc.perform(patch(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_UPDATE_PASSWORD_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(passwordUpdateDTO)))
                    .andExpect(status().isNoContent());

            assertThat(userRepository.findById(user.getId()))
                    .isPresent()
                    .get()
                    .satisfies(updatedUser -> {
                            assertThat(passwordEncoder.matches(passwordUpdateDTO.getNewPassword(), updatedUser.getPassword())).isTrue();
                            assertThat(updatedUser.getTokenVersion()).isEqualTo(user.getTokenVersion() + 1);
                    });
        }

        @Test
        @DisplayName("Should return 400, when the password input is invalid")
        @WithMockBlogUser(USERNAME)
        void  shouldReturn400WhenPasswordInputIsInvalid() throws Exception {
            PasswordUpdateDTO passwordUpdateDTO = PasswordUpdateDTO.builder()
                    .oldPassword("testPassword")
                    .newPassword("newpassword")
                    .build();

            MvcResult response = mockMvc.perform(patch(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_UPDATE_PASSWORD_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(passwordUpdateDTO)))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.validation.failed"));
            assertThat(errorResponse.getSubErrors())
                    .hasSize(2)
                    .extracting(ApiError.FieldError::getField, ApiError.FieldError::getMessage)
                    .containsExactlyInAnyOrder(
                            tuple("oldPassword", messageService.get("validation.user.password")),
                            tuple("newPassword", messageService.get("validation.user.password"))
                    );

            assertThat(userRepository.findById(user.getId()))
                    .isPresent()
                    .get()
                    .satisfies(updatedUser -> {
                            assertThat(passwordEncoder.matches(passwordUpdateDTO.getNewPassword(), user.getPassword())).isFalse();
                            assertThat(updatedUser.getTokenVersion()).isNotEqualTo(user.getTokenVersion() + 1);
                    });
        }

        @Test
        @DisplayName("Should return 401, when the old password is incorrect")
        @WithMockBlogUser(USERNAME)
        void shouldReturn401WhenOldPasswordIsInvalid() throws Exception {
            PasswordUpdateDTO passwordUpdateDTO = PasswordUpdateDTO.builder()
                    .oldPassword("InvalidPassword@123")
                    .newPassword("NewPassword@123")
                    .build();

            MvcResult response = mockMvc.perform(patch(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_UPDATE_PASSWORD_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(passwordUpdateDTO)))
                    .andExpect(status().isUnauthorized())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.auth.bad_credentials", "Old password"));
            assertThat(userRepository.findById(user.getId()))
                    .isPresent()
                    .get()
                    .satisfies(updatedUser -> {
                            assertThat(passwordEncoder.matches(passwordUpdateDTO.getNewPassword(), user.getPassword())).isFalse();
                            assertThat(updatedUser.getTokenVersion()).isNotEqualTo(user.getTokenVersion() + 1);
                    });
        }

        @Test
        @DisplayName("Should return 401 when the user is unauthenticated")
        void shouldReturn401WhenUserIsUnauthenticated() throws Exception {
            PasswordUpdateDTO passwordUpdateDTO = PasswordUpdateDTO.builder()
                    .oldPassword(PASSWORD)
                    .newPassword("NewPassword@123")
                    .build();

            MvcResult response = mockMvc.perform(patch(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_UPDATE_PASSWORD_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(passwordUpdateDTO)))
                    .andExpect(status().isUnauthorized())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo("Full authentication is required to access this resource");

            assertThat(userRepository.findById(user.getId()))
                    .isPresent()
                    .get()
                    .satisfies(updatedUser -> {
                            assertThat(passwordEncoder.matches(passwordUpdateDTO.getNewPassword(), user.getPassword())).isFalse();
                            assertThat(updatedUser.getTokenVersion()).isNotEqualTo(user.getTokenVersion() + 1);
                    });
        }
    }

    @Nested
    @DisplayName("PATCH " +  ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_UPDATE_USERNAME_PATH)
    class UpdateUserName {

        @Test
        @DisplayName("Should return 204, and updates username")
        @WithMockBlogUser(USERNAME)
        void shouldReturn204AndUpdateUserUsername() throws Exception {
            UsernameUpdateDTO usernameUpdateDTO = UsernameUpdateDTO.builder()
                    .username("updatedUsername")
                    .build();

            mockMvc.perform(patch(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_UPDATE_USERNAME_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(usernameUpdateDTO)))
                    .andExpect(status().isNoContent());

            assertThat(userRepository.findById(user.getId()))
                    .isPresent()
                    .get()
                            .satisfies(updatedUser -> {
                                assertThat(updatedUser.getUsername()).isEqualTo(usernameUpdateDTO.getUsername());
                                assertThat(updatedUser.getTokenVersion()).isEqualTo(user.getTokenVersion() + 1);
                            });
        }

        @Test
        @DisplayName("Should return 400, when the username input is invalid")
        @WithMockBlogUser(USERNAME)
        void  shouldReturn400WhenUsernameInputIsInvalid() throws Exception {
            UsernameUpdateDTO usernameUpdateDTO = UsernameUpdateDTO.builder()
                    .username("user-name")
                    .build();

            MvcResult response = mockMvc.perform(patch(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_UPDATE_USERNAME_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(usernameUpdateDTO)))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.validation.failed"));
            assertThat(errorResponse.getSubErrors())
                    .hasSize(1)
                    .extracting(ApiError.FieldError::getField, ApiError.FieldError::getMessage)
                    .containsExactlyInAnyOrder(tuple("username", messageService.get("validation.user.username")));

            assertThat(userRepository.findById(user.getId()))
                    .isPresent()
                    .get()
                    .satisfies(updatedUser -> {
                            assertThat(updatedUser.getUsername()).isNotEqualTo(usernameUpdateDTO.getUsername());
                            assertThat(updatedUser.getTokenVersion()).isNotEqualTo(user.getTokenVersion() + 1);
                    });
        }

        @Test
        @DisplayName("Should return 400, when the username input is null")
        @WithMockBlogUser(USERNAME)
        void shouldReturn400WhenUserNameInputIsNull() throws Exception {
            UsernameUpdateDTO usernameUpdateDTO = UsernameUpdateDTO.builder()
                    .build();

            MvcResult response = mockMvc.perform(patch(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_UPDATE_USERNAME_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(usernameUpdateDTO)))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.validation.failed"));

            assertThat(errorResponse.getSubErrors())
                    .hasSize(1)
                    .extracting(ApiError.FieldError::getField, ApiError.FieldError::getMessage)
                    .containsExactlyInAnyOrder(tuple("username", messageService.get("validation.user.username.not_blank")));

            assertThat(userRepository.findById(user.getId()))
                    .isPresent()
                    .get()
                    .satisfies(updatedUser -> {
                        assertThat(updatedUser.getUsername()).isNotEqualTo(usernameUpdateDTO.getUsername());
                        assertThat(updatedUser.getTokenVersion()).isNotEqualTo(user.getTokenVersion() + 1);
                    });
        }

        @Test
        @DisplayName("Should return 401 when the user is unauthenticated")
        void shouldReturn401WhenUserIsUnauthenticated() throws Exception {
            UsernameUpdateDTO usernameUpdateDTO = UsernameUpdateDTO.builder()
                    .username("updatedUsername")
                    .build();

            MvcResult response = mockMvc.perform(patch(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_UPDATE_USERNAME_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(usernameUpdateDTO)))
                    .andExpect(status().isUnauthorized())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo("Full authentication is required to access this resource");

            assertThat(userRepository.findById(user.getId()))
                    .isPresent()
                    .get()
                    .satisfies(updatedUser -> {
                            assertThat(updatedUser.getUsername()).isNotEqualTo(usernameUpdateDTO.getUsername());
                            assertThat(updatedUser.getTokenVersion()).isNotEqualTo(user.getTokenVersion() + 1);
                    });
        }

        @Test
        @DisplayName("Should return 409 when the username is already taken")
        @WithMockBlogUser(USERNAME)
        void shouldReturn409WhenUsernameIsAlreadyTaken() throws Exception {
            UserEntity existingUser = userRepository.saveAndFlush(testDataFactory.createCustomUser("existingUser", "existingUser@example.com", "existingPassword@123"));

            UsernameUpdateDTO usernameUpdateDTO = UsernameUpdateDTO.builder()
                    .username(existingUser.getUsername())
                    .build();

            MvcResult response = mockMvc.perform(patch(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_UPDATE_USERNAME_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(usernameUpdateDTO)))
                    .andExpect(status().isConflict())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.conflict", "Username"));

            assertThat(userRepository.findById(user.getId()))
                    .isPresent()
                    .get()
                    .satisfies(updatedUser -> {
                            assertThat(updatedUser.getUsername()).isNotEqualTo(usernameUpdateDTO.getUsername());
                            assertThat(updatedUser.getTokenVersion()).isNotEqualTo(user.getTokenVersion() + 1);
                    });
        }
    }

    @Nested
    @DisplayName("PATCH " + ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_UPDATE_EMAIL_PATH)
    class UpdateEmail {

        @Test
        @DisplayName("Should return 204, and updates email")
        @WithMockBlogUser(USERNAME)
        void shouldReturn204AndUpdateUserEmail() throws Exception {
            EmailUpdateDTO emailUpdateDTO = EmailUpdateDTO.builder()
                    .email("newemail@gmail.com")
                    .build();

           mockMvc.perform(patch(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_UPDATE_EMAIL_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(emailUpdateDTO)))
                    .andExpect(status().isNoContent());

           assertThat(userRepository.findById(user.getId()))
                   .isPresent()
                   .get()
                   .satisfies(updatedUser -> {
                       assertThat(updatedUser.getEmail()).isEqualTo(emailUpdateDTO.getEmail());
                       assertThat(updatedUser.getTokenVersion()).isEqualTo(user.getTokenVersion() + 1);
                   });
        }

        @Test
        @DisplayName("Should return 400, when the email input is blank")
        @WithMockBlogUser(USERNAME)
        void  shouldReturn400WhenEmailInputIsBlank() throws Exception {
            EmailUpdateDTO emailUpdateDTO = EmailUpdateDTO.builder()
                    .build();

            MvcResult response = mockMvc.perform(patch(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_UPDATE_EMAIL_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(emailUpdateDTO)))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.validation.failed"));
            assertThat(errorResponse.getSubErrors())
                    .hasSize(1)
                    .extracting(ApiError.FieldError::getField, ApiError.FieldError::getMessage)
                    .containsExactlyInAnyOrder(
                            tuple("email", messageService.get("validation.user.email.not_blank"))
                    );

            assertThat(userRepository.findById(user.getId()))
                   .isPresent()
                   .get()
                   .satisfies(updatedUser -> {
                       assertThat(updatedUser.getEmail()).isNotEqualTo(emailUpdateDTO.getEmail());
                       assertThat(updatedUser.getTokenVersion()).isNotEqualTo(user.getTokenVersion() + 1);
                   });
        }

        @Test
        @DisplayName("Should return 400, when the email input is invalid")
        @WithMockBlogUser(USERNAME)
        void  shouldReturn400WhenEmailInputIsInvalid() throws Exception {
            EmailUpdateDTO emailUpdateDTO = EmailUpdateDTO.builder()
                    .email("invalid-email")
                    .build();

            MvcResult response = mockMvc.perform(patch(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_UPDATE_EMAIL_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(emailUpdateDTO)))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            ApiError errorResponse =  testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.validation.failed"));
            assertThat(errorResponse.getSubErrors())
                    .hasSize(1)
                    .extracting(ApiError.FieldError::getField, ApiError.FieldError::getMessage)
                    .containsExactlyInAnyOrder(tuple("email", messageService.get("validation.user.email.invalid")));

            assertThat(userRepository.findById(user.getId()))
                    .isPresent()
                    .get()
                    .satisfies(updatedUser -> {
                        assertThat(updatedUser.getEmail()).isNotEqualTo(emailUpdateDTO.getEmail());
                        assertThat(updatedUser.getTokenVersion()).isNotEqualTo(user.getTokenVersion() + 1);
                    });
        }

        @Test
        @DisplayName("Should return 401, when the user is unauthenticated")
        void shouldReturn401WhenUserIsUnauthenticated() throws Exception {
            EmailUpdateDTO emailUpdateDTO = EmailUpdateDTO.builder()
                    .email("newemail@gmail.com")
                    .build();

            MvcResult response = mockMvc.perform(patch(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_UPDATE_EMAIL_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(emailUpdateDTO)))
                    .andExpect(status().isUnauthorized())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo("Full authentication is required to access this resource");

            assertThat(userRepository.findById(user.getId()))
                    .isPresent()
                    .get()
                    .satisfies(updatedUser -> {
                        assertThat(updatedUser.getEmail()).isNotEqualTo(emailUpdateDTO.getEmail());
                        assertThat(updatedUser.getTokenVersion()).isNotEqualTo(user.getTokenVersion() + 1);
                    });
        }

        @Test
        @DisplayName("Should return 409, when the email is already taken")
        @WithMockBlogUser(USERNAME)
        void shouldReturn409WhenEmailIsAlreadyTaken() throws Exception {
            UserEntity existingUser = userRepository.saveAndFlush(testDataFactory.createCustomUser("existingUser", "existinguser@gmail.com", "existingPassword@123"));

            EmailUpdateDTO emailUpdateDTO = EmailUpdateDTO.builder()
                    .email(existingUser.getEmail())
                    .build();

            MvcResult response = mockMvc.perform(patch(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_UPDATE_EMAIL_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(emailUpdateDTO)))
                    .andExpect(status().isConflict())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.conflict", "Email"));

            assertThat(userRepository.findById(user.getId()))
                    .isPresent()
                    .get()
                    .satisfies(updatedUser -> {
                        assertThat(updatedUser.getEmail()).isNotEqualTo(emailUpdateDTO.getEmail());
                        assertThat(updatedUser.getTokenVersion()).isNotEqualTo(user.getTokenVersion() + 1);
                    });
        }
    }

    @Nested
    @DisplayName("GET " + ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_PATH_VARIABLE)
    class GetUserProfile {
        @Test
        @DisplayName("Should return 200, when user profile is returned.")
        @WithMockBlogUser(USERNAME)
        void shouldReturn200WhenUserProfileIsReturned() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_PATH_VARIABLE, user.getUsername(), user.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            UserDTO userDTO = testResponseExtractor.extractPayload(response, UserDTO.class);

            assertThat(userDTO.getUsername()).isEqualTo(user.getUsername());
            assertThat(userDTO.getName()).isEqualTo(user.getName());
            assertThat(userDTO.getBio()).isEqualTo(user.getBio());
            assertThat(userDTO.getId()).isEqualTo(user.getId());
            assertThat(userDTO.getNoOfFollowers()).isEqualTo(user.getNoOfFollowers());
            assertThat(userDTO.getNoOfFollowings()).isEqualTo(user.getNoOfFollowings());
            assertThat(userDTO.getIsDeleted()).isFalse();
            assertThat(userDTO.getIsCurrentUser()).isTrue();
        }

        @Test
        @DisplayName("Should return 404, when the user profile is not found")
        @WithMockBlogUser(USERNAME)
        void shouldReturn404WhenUserProfileIsNotFound() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_PATH_VARIABLE, "nonExistentUser", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.not_found", "User account"));
        }

        @Test
        @DisplayName("Should return 401, when the user is unauthenticated")
        void shouldReturn401WhenUserIsUnauthenticated() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_PATH_VARIABLE, user.getUsername(), user.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo("Full authentication is required to access this resource");
        }
    }

    @Nested
    @DisplayName("GET " + ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_SEARCH_PATH)
    class SearchUsers {

        UserEntity user1, user2, user3, user4, user5;

        @BeforeEach
        void addUsers() {
            user1 = userRepository.saveAndFlush(testDataFactory.createUser()
                    .name("Search User1")
                    .username("SearchUser1")
                    .email("searchuser1@gmail.com")
                    .build());

            user2 = userRepository.saveAndFlush(testDataFactory.createUser()
                    .name("Search User2")
                    .username("SearchUser2")
                    .email("searchuser2@gmail.com")
                    .build());

            user3 = userRepository.saveAndFlush(testDataFactory.createUser()
                    .name("Search User3")
                    .username("SearchUser3")
                    .email("searchuser3@gmail.com")
                    .build());

            user4 = userRepository.saveAndFlush(testDataFactory.createUser()
                    .name("Search User4")
                    .username("SearchUser4")
                    .email("searchuser4@gmail.com")
                    .build());

            user5 = userRepository.saveAndFlush(testDataFactory.createUser()
                    .name("User5")
                    .username("User5")
                    .email("user5@gmail.com")
                    .build());
        }

        @Test
        @DisplayName("Should return 200, when a list of users are returned")
        @WithMockBlogUser(USERNAME)
        void shouldReturn200WhenAListOfUsersAreReturned() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_SEARCH_PATH)
                            .param("query", "Search")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            List<UserInfoDTO> userList = testResponseExtractor.extractListPayload(response, UserInfoDTO.class);
            assertThat(userList)
                    .hasSize(4)
                    .extracting(UserInfoDTO::getUsername)
                    .containsExactlyInAnyOrder(
                            user1.getUsername(),
                            user2.getUsername(),
                            user3.getUsername(),
                            user4.getUsername()
                    );
        }

        @Test
        @DisplayName("should return 200, and an empty list, when no user matches the search")
        @WithMockBlogUser(USERNAME)
        void shouldReturn200AndNoUsersAreReturnedWhenNoUserMatches() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_SEARCH_PATH)
                            .param("query", "NonExistentUser")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            List<UserInfoDTO> userList = testResponseExtractor.extractListPayload(response, UserInfoDTO.class);
            assertThat(userList).hasSize(0);
        }

        @Test
        @DisplayName("Should return 401, when the user is unauthenticated")
        void shouldReturn401WhenUserIsUnauthenticated() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_SEARCH_PATH)
                            .param("query", "Search")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo("Full authentication is required to access this resource");
        }
    }

    @Nested
    @DisplayName("POST " + ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_FOLLOW_PATH)
    class FollowOrUnfollowUser {

        UserEntity followee;

        @BeforeEach
        void addFollowee() {
            followee = userRepository.saveAndFlush(testDataFactory.createUser()
                    .name("Followee")
                    .username("Followee")
                    .email("followee@gmail.com")
                    .build());
        }

        @Test
        @DisplayName("Should return 204, when the user has followed successfully")
        @WithMockBlogUser(USERNAME)
        void shouldReturn204WhenUserHasFollowedSuccessfully() throws Exception {
            FollowDTO followDTO = new FollowDTO(true);

            mockMvc.perform(post(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_FOLLOW_PATH, followee.getUsername(), followee.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(followDTO)))
                    .andExpect(status().isNoContent());

            assertThat(followRepository.findByFollowerIdAndFollowingId(user.getId(), followee.getId())).isPresent();
        }

        @Test
        @DisplayName("Should return 204, when the user has unfollowed successfully")
        @WithMockBlogUser(USERNAME)
        void shouldReturn204WhenUserHasUnfollowedSuccessfully() throws Exception {
            followRepository.saveAndFlush(FollowEntity.builder()
                    .follower(user)
                    .following(followee)
                    .build());

            FollowDTO followDTO = new FollowDTO(false);

            mockMvc.perform(post(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_FOLLOW_PATH, followee.getUsername(), followee.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(followDTO)))
                    .andExpect(status().isNoContent());

            assertThat(followRepository.findByFollowerIdAndFollowingId(user.getId(), followee.getId())).isNotPresent();
        }

        @Test
        @DisplayName("Should return 204, when follow action is already performed")
        @WithMockBlogUser(USERNAME)
        void shouldReturn204WhenUserFollowActionIsAlreadyPerformed() throws Exception {
            followRepository.saveAndFlush(FollowEntity.builder()
                    .follower(user)
                    .following(followee)
                    .build());

            FollowDTO followDTO = new FollowDTO(true);

            mockMvc.perform(post(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_FOLLOW_PATH, followee.getUsername(), followee.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(followDTO)))
                    .andExpect(status().isNoContent());

            assertThat(followRepository.findByFollowerIdAndFollowingId(user.getId(), followee.getId())).isPresent();
        }

        @Test
        @DisplayName("Should return 204, when unfollow action is already performed")
        @WithMockBlogUser(USERNAME)
        void shouldReturn204WhenUserUnfollowActionIsAlreadyPerformed() throws Exception {
            FollowDTO followDTO = new FollowDTO(false);

            mockMvc.perform(post(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_FOLLOW_PATH, followee.getUsername(), followee.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(followDTO)))
                    .andExpect(status().isNoContent());

            assertThat(followRepository.findByFollowerIdAndFollowingId(user.getId(), followee.getId())).isNotPresent();
        }

        @Test
        @DisplayName("Should return 400, when follow data is invalid")
        @WithMockBlogUser(USERNAME)
        void shouldReturn400WhenFollowDataIsInvalid() throws Exception {
            FollowDTO followDTO = new FollowDTO(null);

            MvcResult response = mockMvc.perform(post(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_FOLLOW_PATH, followee.getUsername(), followee.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(followDTO)))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.validation.failed"));
            assertThat(errorResponse.getSubErrors())
                    .hasSize(1)
                    .extracting(ApiError.FieldError::getField, ApiError.FieldError::getMessage)
                    .containsExactlyInAnyOrder(tuple("follow", messageService.get("validation.follow.follow.not_null")));

            assertThat(followRepository.findByFollowerIdAndFollowingId(user.getId(), followee.getId())).isNotPresent();
        }

        @Test
        @DisplayName("Should return 401, when the user is unauthenticated")
        void shouldReturn401WhenUserIsUnauthenticated() throws Exception {
            FollowDTO followDTO = new FollowDTO(true);

            MvcResult response = mockMvc.perform(post(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_FOLLOW_PATH, followee.getUsername(), followee.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(followDTO)))
                    .andExpect(status().isUnauthorized())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo("Full authentication is required to access this resource");

            assertThat(followRepository.findByFollowerIdAndFollowingId(user.getId(), followee.getId())).isNotPresent();
        }

        @Test
        @DisplayName("Should return 404, when the user is not found")
        @WithMockBlogUser(USERNAME)
        void shouldReturn404WhenUserIsNotFound() throws Exception {
            FollowDTO followDTO = new FollowDTO(true);

            MvcResult response = mockMvc.perform(post(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_FOLLOW_PATH, "nonexistentusername", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(followDTO)))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.not_found", "User account"));
        }

        @Test
        @DisplayName("Should return 406, when user tries to follow oneself")
        @WithMockBlogUser(USERNAME)
        void shouldReturn406WhenUserTriesToFollowOneself() throws Exception {
            FollowDTO followDTO = new FollowDTO(true);

            MvcResult response = mockMvc.perform(post(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_FOLLOW_PATH, user.getUsername(), user.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(followDTO)))
                    .andExpect(status().isNotAcceptable())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.invalid.action.self_follow"));

            assertThat(followRepository.findByFollowerIdAndFollowingId(user.getId(), user.getId()).isPresent());
        }
    }

    @Nested
    @DisplayName("GET " + ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_FOLLOWERS_PATH)
    class GetFollowers {

        @Test
        @DisplayName("Should return 200, and the followers list")
        @WithMockBlogUser(USERNAME)
        void getShouldReturn200AndFollowersList() throws Exception {
            UserEntity follower1 = userRepository.saveAndFlush(testDataFactory.createUser()
                    .name("Follower1")
                    .username("Follower1")
                    .email("follower1@gmail.com")
                    .build());

            UserEntity follower2 = userRepository.saveAndFlush(testDataFactory.createUser()
                    .name("Follower2")
                    .username("Follower2")
                    .email("follower2@gmail.com")
                    .build());

            UserEntity follower3 = userRepository.saveAndFlush(testDataFactory.createUser()
                    .name("Follower3")
                    .username("Follower3")
                    .email("follower3@gmail.com")
                    .build());

            followRepository.saveAndFlush(FollowEntity.builder()
                    .follower(follower1)
                    .following(user)
                    .build());

            followRepository.saveAndFlush(FollowEntity.builder()
                    .follower(follower2)
                    .following(user)
                    .build());

            followRepository.saveAndFlush(FollowEntity.builder()
                    .follower(follower3)
                    .following(user)
                    .build());

            MvcResult response = mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_FOLLOWERS_PATH, user.getUsername(), user.getId()))
                    .andExpect(status().isOk())
                    .andReturn();

            TestSliceResponse<FollowInfoDTO> sliceResponse = testResponseExtractor.extractSlicePayload(response, FollowInfoDTO.class);
            assertThat(sliceResponse.getContent())
                    .hasSize(3)
                    .extracting(FollowInfoDTO::getUser)
                    .extracting(UserInfoDTO::getUsername)
                    .containsExactlyInAnyOrder(
                            follower1.getUsername(),
                            follower2.getUsername(),
                            follower3.getUsername()
                    );
            assertThat(sliceResponse.isEmpty()).isFalse();
            assertThat(sliceResponse.isFirst()).isTrue();
            assertThat(sliceResponse.isLast()).isTrue();
            assertThat(sliceResponse.getNumberOfElements()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should return 200, and empty list, when no followers exist")
        @WithMockBlogUser(USERNAME)
        void getShouldReturn200AndEmptyListWhenNoFollowersExist() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_FOLLOWERS_PATH, user.getUsername(), user.getId()))
                    .andExpect(status().isOk())
                    .andReturn();

            TestSliceResponse<FollowInfoDTO> sliceResponse = testResponseExtractor.extractSlicePayload(response, FollowInfoDTO.class);
            assertThat(sliceResponse.getContent()).isEmpty();
            assertThat(sliceResponse.isEmpty()).isTrue();
            assertThat(sliceResponse.isFirst()).isTrue();
            assertThat(sliceResponse.isLast()).isTrue();
            assertThat(sliceResponse.getNumberOfElements()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should return 404, when the user does not exist")
        @WithMockBlogUser(USERNAME)
        void shouldReturn404WhenUserDoesNotExist() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_FOLLOWERS_PATH, "NonExistentUser", UUID.randomUUID()))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.not_found", "User account"));
        }

        @Test
        @DisplayName("Should return 401, when the user is unauthenticated")
        void shouldReturn401WhenUserIsUnauthenticated() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_FOLLOWERS_PATH, user.getUsername(), user.getId()))
                    .andExpect(status().isUnauthorized())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo("Full authentication is required to access this resource");
        }
    }

    @Nested
    @DisplayName("GET " + ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_FOLLOWINGS_PATH)
    class GetFollowings {

        @Test
        @DisplayName("Should return 200, and the followings list")
        @WithMockBlogUser(USERNAME)
        void getShouldReturn200AndFollowingsList() throws Exception {
            UserEntity following1 = userRepository.saveAndFlush(testDataFactory.createUser()
                    .name("Following1")
                    .username("Following1")
                    .email("following1@gmail.com")
                    .build());

            UserEntity following2 = userRepository.saveAndFlush(testDataFactory.createUser()
                    .name("Following2")
                    .username("Following2")
                    .email("following2@gmail.com")
                    .build());

            UserEntity following3 = userRepository.saveAndFlush(testDataFactory.createUser()
                    .name("Following3")
                    .username("Following3")
                    .email("following3@gmail.com")
                    .build());

            followRepository.saveAndFlush(FollowEntity.builder()
                    .follower(user)
                    .following(following1)
                    .build());

            followRepository.saveAndFlush(FollowEntity.builder()
                    .follower(user)
                    .following(following2)
                    .build());

            followRepository.saveAndFlush(FollowEntity.builder()
                    .follower(user)
                    .following(following3)
                    .build());

            MvcResult response = mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_FOLLOWINGS_PATH, user.getUsername(), user.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            TestSliceResponse<FollowInfoDTO> sliceResponse = testResponseExtractor.extractSlicePayload(response, FollowInfoDTO.class);
            assertThat(sliceResponse.getContent())
                    .hasSize(3)
                    .extracting(FollowInfoDTO::getUser)
                    .extracting(UserInfoDTO::getUsername)
                    .containsExactlyInAnyOrder(
                            following1.getUsername(),
                            following2.getUsername(),
                            following3.getUsername()
                    );
            assertThat(sliceResponse.isEmpty()).isFalse();
            assertThat(sliceResponse.isFirst()).isTrue();
            assertThat(sliceResponse.isLast()).isTrue();
            assertThat(sliceResponse.getNumberOfElements()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should return 200, and empty list, when no followings exist")
        @WithMockBlogUser(USERNAME)
        void getShouldReturn200AndEmptyListWhenNoFollowingsExist() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_FOLLOWINGS_PATH, user.getUsername(), user.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            TestSliceResponse<FollowInfoDTO> sliceResponse = testResponseExtractor.extractSlicePayload(response, FollowInfoDTO.class);
            assertThat(sliceResponse.getContent()).isEmpty();
            assertThat(sliceResponse.isEmpty()).isTrue();
            assertThat(sliceResponse.isFirst()).isTrue();
            assertThat(sliceResponse.isLast()).isTrue();
            assertThat(sliceResponse.getNumberOfElements()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should return 404, when the user does not exist")
        @WithMockBlogUser(USERNAME)
        void shouldReturn404WhenUserDoesNotExist() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_FOLLOWINGS_PATH, "NonExistentUser",UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.not_found", "User account"));
        }

        @Test
        @DisplayName("Should return 401, when the user is unauthenticated")
        void shouldReturn401WhenUserIsUnauthenticated() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_FOLLOWINGS_PATH, user.getUsername(), user.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo("Full authentication is required to access this resource");
        }
    }

    @Nested
    @DisplayName("DELETE " + ApiRoutes.USERS_BASE_PATH)
    class DeleteUser {

        @Test
        @DisplayName("Should return 204 and the user is deleted successfully")
        @WithMockBlogUser(USERNAME)
        void shouldReturn204AndUserIsDeletedSuccessfully() throws Exception {
            mockMvc.perform(delete(ApiRoutes.USERS_BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            assertThat(userRepository.findById(user.getId()))
                    .isPresent()
                    .get()
                    .satisfies(deletedUser -> assertThat(deletedUser.getActive()).isFalse());
        }

        @Test
        @DisplayName("Should return 401, when the user is unauthenticated")
        void shouldReturn401WhenUserIsUnauthenticated() throws Exception {
            MvcResult response = mockMvc.perform(delete(ApiRoutes.USERS_BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo("Full authentication is required to access this resource");

            assertThat(userRepository.findById(user.getId()))
                    .isPresent()
                    .get()
                    .satisfies(deletedUser -> assertThat(deletedUser.getActive()).isTrue());
        }
    }

    @Nested
    @DisplayName("GET " + ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_POSTS_PATH)
    class GetUserPosts {

        @Test
        @DisplayName("Should return 200, and post list")
        @WithMockBlogUser(USERNAME)
        void shouldReturn200AndPostListSuccessfully() throws Exception {
            CategoryEntity category = categoryRepository.saveAndFlush(testDataFactory.createCategory()
                    .name("Test Category")
                    .slug("test-category")
                    .build());

            PostEntity post1 = postRepository.saveAndFlush(testDataFactory.createPost()
                    .title("Test Post 1")
                    .user(user)
                    .category(category)
                    .build());

            PostEntity post2 = postRepository.saveAndFlush(testDataFactory.createPost()
                    .title("Test Post 2")
                    .user(user)
                    .category(category)
                    .build());

            PostEntity post3 = postRepository.saveAndFlush(testDataFactory.createPost()
                    .title("Test Post 3")
                    .user(user)
                    .category(category)
                    .build());

            MvcResult response = mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_POSTS_PATH, user.getUsername(), user.getId()))
                    .andExpect(status().isOk())
                    .andReturn();

            TestSliceResponse<PostInfoDTO> sliceResponse = testResponseExtractor.extractSlicePayload(response, PostInfoDTO.class);
            assertThat(sliceResponse.getContent())
                    .hasSize(3)
                    .extracting(PostInfoDTO::getTitle)
                    .containsExactlyInAnyOrder(
                            post1.getTitle(),
                            post2.getTitle(),
                            post3.getTitle()
                    );

            assertThat(sliceResponse.isEmpty()).isFalse();
            assertThat(sliceResponse.isFirst()).isTrue();
            assertThat(sliceResponse.isLast()).isTrue();
            assertThat(sliceResponse.getNumberOfElements()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should return 200, and empty post list, when no posts by the user")
        @WithMockBlogUser(USERNAME)
        void shouldReturn200AndEmptyPostListWhenNoPostsExist() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_POSTS_PATH, user.getUsername(), user.getId()))
                    .andExpect(status().isOk())
                    .andReturn();

            TestSliceResponse<PostInfoDTO> sliceResponse = testResponseExtractor.extractSlicePayload(response, PostInfoDTO.class);
            assertThat(sliceResponse.getContent()).isEmpty();
            assertThat(sliceResponse.isEmpty()).isTrue();
            assertThat(sliceResponse.isFirst()).isTrue();
            assertThat(sliceResponse.isLast()).isTrue();
            assertThat(sliceResponse.getNumberOfElements()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should return 404, when the user does not exist")
        @WithMockBlogUser(USERNAME)
        void shouldReturn404WhenUserDoesNotExist() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_POSTS_PATH, "nonexistentuser", UUID.randomUUID()))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.not_found", "User account"));
        }

        @Test
        @DisplayName("Should return 401, when the user is unauthenticated")
        void shouldReturn401WhenUserIsUnauthenticated() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_POSTS_PATH, user.getUsername(), user.getId()))
                    .andExpect(status().isUnauthorized())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo("Full authentication is required to access this resource");
        }
    }

    @Nested
    @DisplayName("GET " + ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_BOOKMARKS_PATH)
    class GetUserBookmarks {

        @Test
        @DisplayName("Should return 200, and user bookmarks list")
        @WithMockBlogUser(USERNAME)
        void shouldReturn200AndUserBookmarksList() throws Exception {
            CategoryEntity category = categoryRepository.saveAndFlush(testDataFactory.createCategory()
                    .name("Test Category")
                    .slug("test-category")
                    .build());

            PostEntity post1 = postRepository.saveAndFlush(testDataFactory.createPost()
                    .title("Test Post 1")
                    .user(user)
                    .category(category)
                    .build());

            PostEntity post2 = postRepository.saveAndFlush(testDataFactory.createPost()
                    .title("Test Post 2")
                    .user(user)
                    .category(category)
                    .build());

            bookmarkRepository.saveAndFlush(BookmarkEntity.builder()
                    .user(user)
                    .post(post1)
                    .build());

            bookmarkRepository.saveAndFlush(BookmarkEntity.builder()
                    .user(user)
                    .post(post2)
                    .build());

            MvcResult response = mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_BOOKMARKS_PATH)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            TestSliceResponse<BookmarkInfoDTO> sliceResponse = testResponseExtractor.extractSlicePayload(response, BookmarkInfoDTO.class);
            assertThat(sliceResponse.getContent())
                    .hasSize(2)
                    .extracting(BookmarkInfoDTO::getPost)
                    .extracting(PostInfoDTO::getTitle)
                    .containsExactlyInAnyOrder(
                            post1.getTitle(),
                            post2.getTitle()
                    );
            assertThat(sliceResponse.isEmpty()).isFalse();
            assertThat(sliceResponse.isFirst()).isTrue();
            assertThat(sliceResponse.isLast()).isTrue();
            assertThat(sliceResponse.getNumberOfElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should return 200, and empty list, when no bookmark exists")
        @WithMockBlogUser(USERNAME)
        void shouldReturn200AndEmptyListWhenNoBookmarksExist() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_BOOKMARKS_PATH)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            TestSliceResponse<BookmarkInfoDTO> sliceResponse = testResponseExtractor.extractSlicePayload(response, BookmarkInfoDTO.class);
            assertThat(sliceResponse.getContent()).hasSize(0);
            assertThat(sliceResponse.isEmpty()).isTrue();
            assertThat(sliceResponse.isFirst()).isTrue();
            assertThat(sliceResponse.isLast()).isTrue();
            assertThat(sliceResponse.getNumberOfElements()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should return 401, when the user is unauthenticated")
        void shouldReturn401WhenUserIsUnauthenticated() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_BOOKMARKS_PATH)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo("Full authentication is required to access this resource");
        }
    }
}
