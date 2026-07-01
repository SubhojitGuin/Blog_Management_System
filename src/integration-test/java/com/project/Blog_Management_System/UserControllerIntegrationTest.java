package com.project.Blog_Management_System;

import com.project.Blog_Management_System.Annotations.WithMockBlogUser;
import com.project.Blog_Management_System.Constants.ApiRoutes;
import com.project.Blog_Management_System.Dto.*;
import com.project.Blog_Management_System.Entities.*;
import com.project.Blog_Management_System.Repositories.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UserControllerIntegrationTest extends BaseIntegrationTest {

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
            ProfileUpdateDTO profileUpdateDTO = ProfileUpdateDTO.builder()
                    .name("Updated User Name")
                    .bio(user.getBio())
                    .gender(user.getGender())
                    .dateOfBirth(user.getDateOfBirth())
                    .build();

            mockMvc.perform(put(ApiRoutes.USERS_BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(profileUpdateDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("Updated User Name"));

            Assertions.assertEquals("Updated User Name", userRepository.findById(user.getId()).get().getName());
        }

        @Test
        @DisplayName("Should return 400 when name and dateOfBirth are null")
        @WithMockBlogUser(USERNAME)
        void shouldReturn400WhenNameAndDateOfBirthAreNull() throws Exception {
            ProfileUpdateDTO profileUpdateDTO = ProfileUpdateDTO.builder()
                    .bio("Updated Bio")
                    .build();

            mockMvc.perform(put(ApiRoutes.USERS_BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(profileUpdateDTO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.subErrors", hasSize(2)))
                    .andExpect(jsonPath("$.error.subErrors[*].field", containsInAnyOrder("name", "dateOfBirth")))
                    .andExpect(jsonPath("$.error.subErrors[*].message", containsInAnyOrder(
                            messageService.get("validation.user.name.not_blank"),
                            messageService.get("validation.user.dob.not_null")
                    )));

            Assertions.assertNotEquals("Updated Bio", userRepository.findById(user.getId()).get().getBio());
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

            mockMvc.perform(put(ApiRoutes.USERS_BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(profileUpdateDTO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.subErrors", hasSize(1)))
                    .andExpect(jsonPath("$.error.subErrors[*].field", containsInAnyOrder("dateOfBirth")))
                    .andExpect(jsonPath("$.error.subErrors[*].message", containsInAnyOrder(
                            messageService.get("validation.user.dob")
                    )));

            Assertions.assertNotEquals("Updated User Name", userRepository.findById(user.getId()).get().getBio());
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

            mockMvc.perform(put(ApiRoutes.USERS_BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(profileUpdateDTO)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.message").value("Full authentication is required to access this resource"));

            Assertions.assertNotEquals("Updated User Name", userRepository.findById(user.getId()).get().getName());
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

            Assertions.assertTrue(passwordEncoder.matches(passwordUpdateDTO.getNewPassword(), userRepository.findById(user.getId()).get().getPassword()));
            Assertions.assertEquals(user.getTokenVersion() + 1, userRepository.findById(user.getId()).get().getTokenVersion());
        }

        @Test
        @DisplayName("Should return 400, when the password input is invalid")
        @WithMockBlogUser(USERNAME)
        void  shouldReturn400WhenPasswordInputIsInvalid() throws Exception {
            PasswordUpdateDTO passwordUpdateDTO = PasswordUpdateDTO.builder()
                    .oldPassword("testPassword")
                    .newPassword("newpassword")
                    .build();

            mockMvc.perform(patch(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_UPDATE_PASSWORD_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(passwordUpdateDTO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.subErrors", hasSize(2)))
                    .andExpect(jsonPath("$.error.subErrors[*].field", containsInAnyOrder("oldPassword", "newPassword")))
                    .andExpect(jsonPath("$.error.subErrors[*].message", containsInAnyOrder(
                            messageService.get("validation.user.password"),
                            messageService.get("validation.user.password")
                    )));

            Assertions.assertFalse(passwordEncoder.matches(passwordUpdateDTO.getNewPassword(), userRepository.findById(user.getId()).get().getPassword()));
            Assertions.assertNotEquals(user.getTokenVersion() + 1, userRepository.findById(user.getId()).get().getTokenVersion());
        }

        @Test
        @DisplayName("Should return 401, when the old password is incorrect")
        @WithMockBlogUser(USERNAME)
        void shouldReturn401WhenOldPasswordIsInvalid() throws Exception {
            PasswordUpdateDTO passwordUpdateDTO = PasswordUpdateDTO.builder()
                    .oldPassword("InvalidPassword@123")
                    .newPassword("NewPassword@123")
                    .build();

            mockMvc.perform(patch(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_UPDATE_PASSWORD_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(passwordUpdateDTO)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.message").value(messageService.get("exception.auth.bad_credentials", "Old password")));

            Assertions.assertFalse(passwordEncoder.matches(passwordUpdateDTO.getNewPassword(), userRepository.findById(user.getId()).get().getPassword()));
            Assertions.assertNotEquals(user.getTokenVersion() + 1, userRepository.findById(user.getId()).get().getTokenVersion());
        }

        @Test
        @DisplayName("Should return 401 when the user is unauthenticated")
        void shouldReturn401WhenUserIsUnauthenticated() throws Exception {
            PasswordUpdateDTO passwordUpdateDTO = PasswordUpdateDTO.builder()
                    .oldPassword(PASSWORD)
                    .newPassword("NewPassword@123")
                    .build();

            mockMvc.perform(patch(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_UPDATE_PASSWORD_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(passwordUpdateDTO)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.message").value("Full authentication is required to access this resource"));

            Assertions.assertFalse(passwordEncoder.matches(passwordUpdateDTO.getNewPassword(), userRepository.findById(user.getId()).get().getPassword()));
            Assertions.assertNotEquals(user.getTokenVersion() + 1, userRepository.findById(user.getId()).get().getTokenVersion());

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

            Assertions.assertEquals(usernameUpdateDTO.getUsername(), userRepository.findById(user.getId()).get().getUsername());
            Assertions.assertEquals(user.getTokenVersion() + 1, userRepository.findById(user.getId()).get().getTokenVersion());
        }

        @Test
        @DisplayName("Should return 400, when the username input is invalid")
        @WithMockBlogUser(USERNAME)
        void  shouldReturn400WhenUsernameInputIsInvalid() throws Exception {
            UsernameUpdateDTO usernameUpdateDTO = UsernameUpdateDTO.builder()
                    .username("user-name")
                    .build();

            mockMvc.perform(patch(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_UPDATE_USERNAME_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(usernameUpdateDTO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.subErrors", hasSize(1)))
                    .andExpect(jsonPath("$.error.subErrors[0].field").value("username"))
                    .andExpect(jsonPath("$.error.subErrors[0].message").value(messageService.get("validation.user.username")));

            Assertions.assertNotEquals(usernameUpdateDTO.getUsername(), userRepository.findById(user.getId()).get().getUsername());
            Assertions.assertNotEquals(user.getTokenVersion() + 1, userRepository.findById(user.getId()).get().getTokenVersion());
        }

        @Test
        @DisplayName("Should return 400, when the username input is null")
        @WithMockBlogUser(USERNAME)
        void shouldReturn400WhenUserNameInputIsNull() throws Exception {
            UsernameUpdateDTO usernameUpdateDTO = UsernameUpdateDTO.builder()
                    .build();

            mockMvc.perform(patch(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_UPDATE_USERNAME_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(usernameUpdateDTO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.subErrors", hasSize(1)))
                    .andExpect(jsonPath("$.error.subErrors[0].field").value("username"))
                    .andExpect(jsonPath("$.error.subErrors[0].message").value(messageService.get("validation.user.username.not_blank")));

            Assertions.assertNotEquals(usernameUpdateDTO.getUsername(), userRepository.findById(user.getId()).get().getUsername());
            Assertions.assertNotEquals(user.getTokenVersion() + 1, userRepository.findById(user.getId()).get().getTokenVersion());
        }

        @Test
        @DisplayName("Should return 401 when the user is unauthenticated")
        void shouldReturn401WhenUserIsUnauthenticated() throws Exception {
            UsernameUpdateDTO usernameUpdateDTO = UsernameUpdateDTO.builder()
                    .username("updatedUsername")
                    .build();

            mockMvc.perform(patch(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_UPDATE_USERNAME_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(usernameUpdateDTO)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.message").value("Full authentication is required to access this resource"));

            Assertions.assertNotEquals(usernameUpdateDTO.getUsername(), userRepository.findById(user.getId()).get().getUsername());
            Assertions.assertNotEquals(user.getTokenVersion() + 1, userRepository.findById(user.getId()).get().getTokenVersion());
        }

        @Test
        @DisplayName("Should return 409 when the username is already taken")
        @WithMockBlogUser(USERNAME)
        void shouldReturn409WhenUsernameIsAlreadyTaken() throws Exception {
            UserEntity existingUser = userRepository.saveAndFlush(testDataFactory.createCustomUser("existingUser", "existingUser@example.com", "existingPassword@123"));

            UsernameUpdateDTO usernameUpdateDTO = UsernameUpdateDTO.builder()
                    .username(existingUser.getUsername())
                    .build();

            mockMvc.perform(patch(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_UPDATE_USERNAME_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(usernameUpdateDTO)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.message").value(messageService.get("exception.resource.conflict", "Username")));

            Assertions.assertNotEquals(usernameUpdateDTO.getUsername(), userRepository.findById(user.getId()).get().getUsername());
            Assertions.assertNotEquals(user.getTokenVersion() + 1, userRepository.findById(user.getId()).get().getTokenVersion());
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

            Assertions.assertEquals(emailUpdateDTO.getEmail(), userRepository.findById(user.getId()).get().getEmail());
            Assertions.assertEquals(user.getTokenVersion() + 1, userRepository.findById(user.getId()).get().getTokenVersion());
        }

        @Test
        @DisplayName("Should return 400, when the email input is blank")
        @WithMockBlogUser(USERNAME)
        void  shouldReturn400WhenEmailInputIsBlank() throws Exception {
            EmailUpdateDTO emailUpdateDTO = EmailUpdateDTO.builder()
                    .build();

            mockMvc.perform(patch(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_UPDATE_EMAIL_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(emailUpdateDTO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.subErrors", hasSize(1)))
                    .andExpect(jsonPath("$.error.subErrors[0].field").value("email"))
                    .andExpect(jsonPath("$.error.subErrors[0].message").value(messageService.get("validation.user.email.not_blank")));

            Assertions.assertNotEquals(emailUpdateDTO.getEmail(), userRepository.findById(user.getId()).get().getEmail());
            Assertions.assertNotEquals(user.getTokenVersion() + 1, userRepository.findById(user.getId()).get().getTokenVersion());
        }

        @Test
        @DisplayName("Should return 400, when the email input is invalid")
        @WithMockBlogUser(USERNAME)
        void  shouldReturn400WhenEmailInputIsInvalid() throws Exception {
            EmailUpdateDTO emailUpdateDTO = EmailUpdateDTO.builder()
                    .email("invalid-email")
                    .build();

            mockMvc.perform(patch(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_UPDATE_EMAIL_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(emailUpdateDTO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.subErrors", hasSize(1)))
                    .andExpect(jsonPath("$.error.subErrors[0].field").value("email"))
                    .andExpect(jsonPath("$.error.subErrors[0].message").value(messageService.get("validation.user.email.invalid")));

            Assertions.assertNotEquals(emailUpdateDTO.getEmail(), userRepository.findById(user.getId()).get().getEmail());
            Assertions.assertNotEquals(user.getTokenVersion() + 1, userRepository.findById(user.getId()).get().getTokenVersion());
        }

        @Test
        @DisplayName("Should return 401, when the user is unauthenticated")
        void shouldReturn401WhenUserIsUnauthenticated() throws Exception {
            EmailUpdateDTO emailUpdateDTO = EmailUpdateDTO.builder()
                    .email("newemail@gmail.com")
                    .build();

            mockMvc.perform(patch(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_UPDATE_EMAIL_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(emailUpdateDTO)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.message").value("Full authentication is required to access this resource"));

            Assertions.assertNotEquals(emailUpdateDTO.getEmail(), userRepository.findById(user.getId()).get().getEmail());
            Assertions.assertNotEquals(user.getTokenVersion() + 1, userRepository.findById(user.getId()).get().getTokenVersion());
        }

        @Test
        @DisplayName("Should return 409, when the email is already taken")
        @WithMockBlogUser(USERNAME)
        void shouldReturn409WhenEmailIsAlreadyTaken() throws Exception {
            UserEntity existingUser = userRepository.saveAndFlush(testDataFactory.createCustomUser("existingUser", "existinguser@gmail.com", "existingPassword@123"));

            EmailUpdateDTO emailUpdateDTO = EmailUpdateDTO.builder()
                    .email(existingUser.getEmail())
                    .build();

            mockMvc.perform(patch(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_UPDATE_EMAIL_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(emailUpdateDTO)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.message").value(messageService.get("exception.resource.conflict", "Email")));

            Assertions.assertNotEquals(emailUpdateDTO.getEmail(), userRepository.findById(user.getId()).get().getEmail());
            Assertions.assertNotEquals(user.getTokenVersion() + 1, userRepository.findById(user.getId()).get().getTokenVersion());
        }
    }

    @Nested
    @DisplayName("GET " + ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_PATH_VARIABLE)
    class GetUserProfile
    {
        @Test
        @DisplayName("Should return 200, when user profile is returned.")
        @WithMockBlogUser(USERNAME)
        void shouldReturn200WhenUserProfileIsReturned() throws Exception {
            mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_PATH_VARIABLE, user.getUsername(), user.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.username").value(user.getUsername()))
                    .andExpect(jsonPath("$.data.name").value(user.getName()))
                    .andExpect(jsonPath("$.data.bio").value(user.getBio()))
                    .andExpect(jsonPath("$.data.id").value(user.getId().toString()))
                    .andExpect(jsonPath("$.data.noOfFollowers").value(user.getNoOfFollowers()))
                    .andExpect(jsonPath("$.data.noOfFollowings").value(user.getNoOfFollowings()))
                    .andExpect(jsonPath("$.data.isDeleted").value(false))
                    .andExpect(jsonPath("$.data.isCurrentUser").value(true));
        }

        @Test
        @DisplayName("Should return 404, when the user profile is not found")
        @WithMockBlogUser(USERNAME)
        void shouldReturn404WhenUserProfileIsNotFound() throws Exception {
            mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_PATH_VARIABLE, "nonExistentUser", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.message").value(messageService.get("exception.resource.not_found", "User account")));
        }

        @Test
        @DisplayName("Should return 401, when the user is unauthenticated")
        void shouldReturn401WhenUserIsUnauthenticated() throws Exception {
            mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_PATH_VARIABLE, user.getUsername(), user.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.message").value("Full authentication is required to access this resource"));
        }
    }

    @Nested
    @DisplayName("GET " + ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_SEARCH_PATH)
    class SearchUsers {

        @BeforeEach
        void addUsers() {
            userRepository.saveAndFlush(testDataFactory.createUser()
                    .name("Search User1")
                    .username("SearchUser1")
                    .email("searchuser1@gmail.com")
                    .build());

            userRepository.saveAndFlush(testDataFactory.createUser()
                    .name("Search User2")
                    .username("SearchUser2")
                    .email("searchuser2@gmail.com")
                    .build());

            userRepository.saveAndFlush(testDataFactory.createUser()
                    .name("Search User3")
                    .username("SearchUser3")
                    .email("searchuser3@gmail.com")
                    .build());

            userRepository.saveAndFlush(testDataFactory.createUser()
                    .name("Search User4")
                    .username("SearchUser4")
                    .email("searchuser4@gmail.com")
                    .build());

            userRepository.saveAndFlush(testDataFactory.createUser()
                    .name("User5")
                    .username("User5")
                    .email("user5@gmail.com")
                    .build());
        }

        @Test
        @DisplayName("Should return 200, when a list of users are returned")
        @WithMockBlogUser(USERNAME)
        void shouldReturn200WhenAListOfUsersAreReturned() throws Exception {
            mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_SEARCH_PATH)
                            .param("query", "Search")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(4)))
                    .andExpect(jsonPath("$.data[*].username", containsInAnyOrder(
                            "SearchUser1",
                            "SearchUser2",
                            "SearchUser3",
                            "SearchUser4"
                    )));
        }

        @Test
        @DisplayName("should return 200, and an empty list, when no user matches the search")
        @WithMockBlogUser(USERNAME)
        void shouldReturn200AndNoUsersAreReturnedWhenNoUserMatches() throws Exception {
            mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_SEARCH_PATH)
                            .param("query", "NonExistentUser")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(0)));
        }

        @Test
        @DisplayName("Should return 401, when the user is unauthenticated")
        void shouldReturn401WhenUserIsUnauthenticated() throws Exception {
            mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_SEARCH_PATH)
                            .param("query", "Search")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.message").value("Full authentication is required to access this resource"));
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

            Assertions.assertTrue(followRepository.findByFollowerIdAndFollowingId(user.getId(), followee.getId()).isPresent());
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

            Assertions.assertFalse(followRepository.findByFollowerIdAndFollowingId(user.getId(), followee.getId()).isPresent());
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

            Assertions.assertTrue(followRepository.findByFollowerIdAndFollowingId(user.getId(), followee.getId()).isPresent());
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

            Assertions.assertFalse(followRepository.findByFollowerIdAndFollowingId(user.getId(), followee.getId()).isPresent());
        }

        @Test
        @DisplayName("Should return 400, when follow data is invalid")
        @WithMockBlogUser(USERNAME)
        void shouldReturn400WhenFollowDataIsInvalid() throws Exception {
            FollowDTO followDTO = new FollowDTO(null);

            mockMvc.perform(post(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_FOLLOW_PATH, followee.getUsername(), followee.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(followDTO)))
                    .andExpect(status().isBadRequest());

            Assertions.assertFalse(followRepository.findByFollowerIdAndFollowingId(user.getId(), followee.getId()).isPresent());
        }

        @Test
        @DisplayName("Should return 401, when the user is unauthenticated")
        void shouldReturn401WhenUserIsUnauthenticated() throws Exception {
            FollowDTO followDTO = new FollowDTO(true);

            mockMvc.perform(post(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_FOLLOW_PATH, followee.getUsername(), followee.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(followDTO)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.message").value("Full authentication is required to access this resource"));

            Assertions.assertFalse(followRepository.findByFollowerIdAndFollowingId(user.getId(), followee.getId()).isPresent());
        }

        @Test
        @DisplayName("Should return 404, when the user is not found")
        @WithMockBlogUser(USERNAME)
        void shouldReturn404WhenUserIsNotFound() throws Exception {
            FollowDTO followDTO = new FollowDTO(true);

            mockMvc.perform(post(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_FOLLOW_PATH, "nonexistentusername", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(followDTO)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.message").value(messageService.get("exception.resource.not_found", "User account")));
        }

        @Test
        @DisplayName("Should return 406, when user tries to follow oneself")
        @WithMockBlogUser(USERNAME)
        void shouldReturn406WhenUserTriesToFollowOneself() throws Exception {
            FollowDTO followDTO = new FollowDTO(true);

            mockMvc.perform(post(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_FOLLOW_PATH, user.getUsername(), user.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(followDTO)))
                    .andExpect(status().isNotAcceptable())
                    .andExpect(jsonPath("$.error.message").value(messageService.get("exception.invalid.action.self_follow")));

            Assertions.assertFalse(followRepository.findByFollowerIdAndFollowingId(user.getId(), user.getId()).isPresent());
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

            mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_FOLLOWERS_PATH, user.getUsername(), user.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(3)))
                    .andExpect(jsonPath("$.data.content[*].user.username", containsInAnyOrder(
                            "Follower1",
                            "Follower2",
                            "Follower3"
                    )));
        }

        @Test
        @DisplayName("Should return 200, and empty list, when no followers exist")
        @WithMockBlogUser(USERNAME)
        void getShouldReturn200AndEmptyListWhenNoFollowersExist() throws Exception {
            mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_FOLLOWERS_PATH, user.getUsername(), user.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(0)));
        }

        @Test
        @DisplayName("Should return 404, when the user does not exist")
        @WithMockBlogUser(USERNAME)
        void shouldReturn404WhenUserDoesNotExist() throws Exception {
            mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_FOLLOWERS_PATH, "NonExistentUser", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.message").value(messageService.get("exception.resource.not_found", "User account")));
        }

        @Test
        @DisplayName("Should return 401, when the user is unauthenticated")
        void shouldReturn401WhenUserIsUnauthenticated() throws Exception {
            mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_FOLLOWERS_PATH, user.getUsername(), user.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.message").value("Full authentication is required to access this resource"));
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

            mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_FOLLOWINGS_PATH, user.getUsername(), user.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(3)))
                    .andExpect(jsonPath("$.data.content[*].user.username", containsInAnyOrder(
                            "Following1",
                            "Following2",
                            "Following3"
                    )));
        }

        @Test
        @DisplayName("Should return 200, and empty list, when no followings exist")
        @WithMockBlogUser(USERNAME)
        void getShouldReturn200AndEmptyListWhenNoFollowingsExist() throws Exception {
            mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_FOLLOWINGS_PATH, user.getUsername(), user.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(0)));
        }

        @Test
        @DisplayName("Should return 404, when the user does not exist")
        @WithMockBlogUser(USERNAME)
        void shouldReturn404WhenUserDoesNotExist() throws Exception {
            mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_FOLLOWINGS_PATH, "NonExistentUser",UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.message").value(messageService.get("exception.resource.not_found", "User account")));
        }

        @Test
        @DisplayName("Should return 401, when the user is unauthenticated")
        void shouldReturn401WhenUserIsUnauthenticated() throws Exception {
            mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_FOLLOWINGS_PATH, user.getUsername(), user.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.message").value("Full authentication is required to access this resource"));
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

            Assertions.assertFalse(userRepository.findById(user.getId()).get().getActive());
        }

        @Test
        @DisplayName("Should return 401, when the user is unauthenticated")
        void shouldReturn401WhenUserIsUnauthenticated() throws Exception {
            mockMvc.perform(delete(ApiRoutes.USERS_BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());

            Assertions.assertTrue(userRepository.findById(user.getId()).get().getActive());
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

            postRepository.saveAndFlush(testDataFactory.createPost()
                    .title("Test Post 1")
                    .user(user)
                    .category(category)
                    .build());

            postRepository.saveAndFlush(testDataFactory.createPost()
                    .title("Test Post 2")
                    .user(user)
                    .category(category)
                    .build());

            postRepository.saveAndFlush(testDataFactory.createPost()
                    .title("Test Post 3")
                    .user(user)
                    .category(category)
                    .build());

            mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_POSTS_PATH, user.getUsername(), user.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(3)))
                    .andExpect(jsonPath("$.data.content[*].title", containsInAnyOrder(
                            "Test Post 1",
                            "Test Post 2",
                            "Test Post 3"
                    )));
        }

        @Test
        @DisplayName("Should return 200, and empty post list, when no posts by the user")
        @WithMockBlogUser(USERNAME)
        void shouldReturn200AndEmptyPostListWhenNoPostsExist() throws Exception {
            mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_POSTS_PATH, user.getUsername(), user.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(0)));
        }

        @Test
        @DisplayName("Should return 404, when the user does not exist")
        @WithMockBlogUser(USERNAME)
        void shouldReturn404WhenUserDoesNotExist() throws Exception {
            mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_POSTS_PATH, "nonexistentuser", UUID.randomUUID()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.message").value(messageService.get("exception.resource.not_found", "User account")));
        }

        @Test
        @DisplayName("Should return 401, when the user is unauthenticated")
        void shouldReturn401WhenUserIsUnauthenticated() throws Exception {
            mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_POSTS_PATH, user.getUsername(), user.getId()))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.message").value("Full authentication is required to access this resource"));
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

            mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_BOOKMARKS_PATH)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(2)))
                    .andExpect(jsonPath("$.data.content[*].post.title", containsInAnyOrder(
                            "Test Post 1",
                            "Test Post 2"
                    )));
        }

        @Test
        @DisplayName("Should return 200, and empty list, when no bookmark exists")
        @WithMockBlogUser(USERNAME)
        void shouldReturn200AndEmptyListWhenNoBookmarksExist() throws Exception {
            mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_BOOKMARKS_PATH)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(0)));
        }

        @Test
        @DisplayName("Should return 401, when the user is unauthenticated")
        void shouldReturn401WhenUserIsUnauthenticated() throws Exception {
            mockMvc.perform(get(ApiRoutes.USERS_BASE_PATH + ApiRoutes.USER_BOOKMARKS_PATH)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.message").value("Full authentication is required to access this resource"));
        }
    }
}
