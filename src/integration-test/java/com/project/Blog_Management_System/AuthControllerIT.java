package com.project.Blog_Management_System;

import com.project.Blog_Management_System.Constants.ApiRoutes;
import com.project.Blog_Management_System.Dto.LoginRequestDTO;
import com.project.Blog_Management_System.Dto.SignUpRequestDTO;
import com.project.Blog_Management_System.Entities.UserEntity;
import com.project.Blog_Management_System.Enums.Gender;
import com.project.Blog_Management_System.Repositories.UserRepository;
import com.project.Blog_Management_System.Security.JWTService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class AuthControllerIT extends BaseIT {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JWTService jwtService;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = userRepository.saveAndFlush(testDataFactory.createUser().build());
    }

    @Nested
    @DisplayName("POST " + ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_SIGNUP)
    class SignupTests {

        private SignUpRequestDTO validSignUpRequest;

        @BeforeEach
        void createSignUpRequest() {
            validSignUpRequest = SignUpRequestDTO.builder()
                    .name("Signup User")
                    .username("signupuser")
                    .email("signupuser@gmail.com")
                    .password("SignUp@123")
                    .gender(Gender.FEMALE)
                    .dateOfBirth(LocalDate.now().minusYears(20))
                    .build();
        }

        @Test
        @DisplayName("Should return 201 and register a new user successfully")
        void shouldRegisterNewUser() throws Exception {
            mockMvc.perform(post(ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_SIGNUP)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validSignUpRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.username", is(validSignUpRequest.getUsername())))
                    .andExpect(jsonPath("$.data.name", is(validSignUpRequest.getName())));

            Assertions.assertTrue(userRepository.findByUsernameIgnoreCase(validSignUpRequest.getUsername()).isPresent());
        }

        @Test
        @DisplayName("Should return 409 conflict when user already exists")
        void shouldReturn409WhenUserAlreadyExists() throws Exception {
            validSignUpRequest.setEmail(user.getEmail());
            validSignUpRequest.setUsername(user.getUsername());

            mockMvc.perform(post(ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_SIGNUP)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validSignUpRequest)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Should return 400 when registration input is null")
        void shouldReturn400WhenInputIsNull() throws Exception {
            SignUpRequestDTO invalidRequest = SignUpRequestDTO.builder()
                    .build();

            mockMvc.perform(post(ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_SIGNUP)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.message").value(messageService.get("exception.validation.failed")))
                    .andExpect(jsonPath("$.error.subErrors", hasSize(5)))
                    .andExpect(jsonPath("$.error.subErrors[*].field", containsInAnyOrder("name", "username", "email", "password", "dateOfBirth")))
                    .andExpect(jsonPath("$.error.subErrors[*].message", containsInAnyOrder(
                            messageService.get("validation.user.name.not_blank"),
                            messageService.get("validation.user.username.not_blank"),
                            messageService.get("validation.user.email.not_blank"),
                            messageService.get("validation.user.password.not_null"),
                            messageService.get("validation.user.dob.not_null")
                    )));

            Assertions.assertFalse(userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(validSignUpRequest.getUsername(), validSignUpRequest.getEmail()).isPresent());
        }

        @Test
        @DisplayName("Should return 400 when registration input fails validation constraints")
        void shouldReturn400WhenInputIsInvalid() throws Exception {
            SignUpRequestDTO invalidRequest = SignUpRequestDTO.builder()
                    .name("i")
                    .username("invalid-username")
                    .email("invalid-email")
                    .password("invalid-password")
                    .dateOfBirth(LocalDate.now().plusDays(1))
                    .build();

            mockMvc.perform(post(ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_SIGNUP)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.message").value(messageService.get("exception.validation.failed")))
                    .andExpect(jsonPath("$.error.subErrors", hasSize(5)))
                    .andExpect(jsonPath("$.error.subErrors[*].field", containsInAnyOrder("name", "username", "email", "password", "dateOfBirth")))
                    .andExpect(jsonPath("$.error.subErrors[*].message", containsInAnyOrder(
                            messageService.get("validation.user.name.size")
                                    .replace("{min}", "2")
                                    .replace("{max}", "255"),
                            messageService.get("validation.user.username"),
                            messageService.get("validation.user.email.invalid"),
                            messageService.get("validation.user.password"),
                            messageService.get("validation.user.dob")
                    )));

            Assertions.assertFalse(userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(validSignUpRequest.getUsername(), validSignUpRequest.getEmail()).isPresent());
        }
    }

    @Nested
    @DisplayName("POST " + ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_LOGIN)
    class LoginTests {

        @Test
        @DisplayName("Should return 200 and login successfully, return access token, and set refresh cookie")
        void shouldLoginSuccessfully() throws Exception {
            LoginRequestDTO loginRequest = LoginRequestDTO.builder()
                    .emailOrUsername(user.getUsername())
                    .password("TestPassword@123")
                    .build();

            mockMvc.perform(post(ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_LOGIN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                    .andExpect(cookie().exists("refreshToken"))
                    .andExpect(cookie().httpOnly("refreshToken", true));
        }

        @Test
        @DisplayName("Should return 401 when using wrong credentials")
        void shouldReturn401ForWrongCredentials() throws Exception {
            LoginRequestDTO wrongLoginRequest = LoginRequestDTO.builder()
                    .emailOrUsername("testuser")
                    .password("WrongPassword123!")
                    .build();

            mockMvc.perform(post(ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_LOGIN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(wrongLoginRequest)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 400 when login input is null")
        void shouldReturn400WhenLoginInputIsNull() throws Exception {
            LoginRequestDTO invalidLoginRequest = LoginRequestDTO.builder()
                    .build();

            mockMvc.perform(post(ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_LOGIN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidLoginRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.message").value(messageService.get("exception.validation.failed")))
                    .andExpect(jsonPath("$.error.subErrors", hasSize(2)))
                    .andExpect(jsonPath("$.error.subErrors[*].field", containsInAnyOrder("emailOrUsername", "password")))
                    .andExpect(jsonPath("$.error.subErrors[*].message", containsInAnyOrder(
                            messageService.get("validation.user.email_or_username.not_null"),
                            messageService.get("validation.user.password.not_null")
                    )));
        }

        @Test
        @DisplayName("Should return 400 when login input fails validation constraints")
        void shouldReturn400WhenLoginInputFailsValidationConstraints() throws Exception {
            LoginRequestDTO invalidLoginRequest = LoginRequestDTO.builder()
                    .emailOrUsername("invalid-email-format")
                    .password("short")
                    .build();

            mockMvc.perform(post(ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_LOGIN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidLoginRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.message").value(messageService.get("exception.validation.failed")))
                    .andExpect(jsonPath("$.error.subErrors", hasSize(2)))
                    .andExpect(jsonPath("$.error.subErrors[*].field", containsInAnyOrder("emailOrUsername", "password")))
                    .andExpect(jsonPath("$.error.subErrors[*].message", containsInAnyOrder(
                            messageService.get("validation.user.email_or_username"),
                            messageService.get("validation.user.password")
                    )));
        }
    }

    @Nested
    @DisplayName("POST " + ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_REFRESH)
    class RefreshTokenTests {

        private Cookie validRefreshTokenCookie;

        @BeforeEach
        void setupUserAndExtractCookie() {
            String refreshToken = jwtService.generateRefreshToken(user);
            validRefreshTokenCookie = new Cookie("refreshToken", refreshToken);
            validRefreshTokenCookie.setHttpOnly(true);
            validRefreshTokenCookie.setMaxAge(3600);
        }

        @Test
        @DisplayName("Should return 200 and successfully issue a new access token using a valid cookie")
        void shouldRefreshTokensWithValidCookie() throws Exception {
            mockMvc.perform(post(ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_REFRESH)
                            .cookie(validRefreshTokenCookie))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                    .andExpect(cookie().exists("refreshToken"));
        }

        @Test
        @DisplayName("Should return 401 when the mandatory refresh token cookie is missing")
        void shouldReturn401WhenCookieIsMissing() throws Exception {
            mockMvc.perform(post(ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_REFRESH))
                    .andExpect(status().isUnauthorized());
        }
    }
}
