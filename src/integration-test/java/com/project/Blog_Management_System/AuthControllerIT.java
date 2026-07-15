package com.project.Blog_Management_System;

import com.project.Blog_Management_System.Advice.ApiError;
import com.project.Blog_Management_System.Constants.ApiRoutes;
import com.project.Blog_Management_System.Dto.LoginRequestDTO;
import com.project.Blog_Management_System.Dto.LoginResponseDTO;
import com.project.Blog_Management_System.Dto.SignUpRequestDTO;
import com.project.Blog_Management_System.Dto.UserDTO;
import com.project.Blog_Management_System.Entities.UserEntity;
import com.project.Blog_Management_System.Enums.Gender;
import com.project.Blog_Management_System.Repositories.UserRepository;
import com.project.Blog_Management_System.Security.JWTService;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Feature("User Authentication")
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
    @Story("User SignUp")
    @Severity(SeverityLevel.BLOCKER)
    class Signup {

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
            MvcResult response = mockMvc.perform(post(ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_SIGNUP)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validSignUpRequest)))
                    .andExpect(status().isCreated())
                    .andReturn();

            UserDTO user = testResponseExtractor.extractPayload(response, UserDTO.class);

            assertThat(user.getUsername()).isEqualTo(validSignUpRequest.getUsername());
            assertThat(user.getName()).isEqualTo(validSignUpRequest.getName());
            assertThat(userRepository.findByUsernameIgnoreCase(validSignUpRequest.getUsername()).isPresent());
        }

        @Test
        @DisplayName("Should return 409 conflict when user already exists")
        void shouldReturn409WhenUserAlreadyExists() throws Exception {
            validSignUpRequest.setEmail(user.getEmail());
            validSignUpRequest.setUsername(user.getUsername());

            MvcResult response = mockMvc.perform(post(ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_SIGNUP)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validSignUpRequest)))
                    .andExpect(status().isConflict())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.conflict", "Username/Email"));

        }

        @Test
        @DisplayName("Should return 400 when registration input is null")
        void shouldReturn400WhenInputIsNull() throws Exception {
            SignUpRequestDTO invalidRequest = SignUpRequestDTO.builder()
                    .build();

            MvcResult response = mockMvc.perform(post(ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_SIGNUP)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.validation.failed"));
            assertThat(errorResponse.getSubErrors())
                    .hasSize(5)
                    .extracting(ApiError.FieldError::getField, ApiError.FieldError::getMessage)
                    .containsExactlyInAnyOrder(
                            tuple("name", messageService.get("validation.user.name.not_blank")),
                            tuple("email", messageService.get("validation.user.email.not_blank")),
                            tuple("username", messageService.get("validation.user.username.not_blank")),
                            tuple("password", messageService.get("validation.user.password.not_null")),
                            tuple("dateOfBirth", messageService.get("validation.user.dob.not_null"))
                    );

            assertThat(userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(validSignUpRequest.getUsername(), validSignUpRequest.getEmail())).isNotPresent();
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

            MvcResult response = mockMvc.perform(post(ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_SIGNUP)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.validation.failed"));
            assertThat(errorResponse.getSubErrors())
                    .hasSize(5)
                    .extracting(ApiError.FieldError::getField, ApiError.FieldError::getMessage)
                    .containsExactlyInAnyOrder(
                            tuple("name", messageService.get("validation.user.name.size")
                                    .replace("{min}", "2")
                                    .replace("{max}", "255")),
                            tuple("username", messageService.get("validation.user.username")),
                            tuple("email", messageService.get("validation.user.email.invalid")),
                            tuple("password", messageService.get("validation.user.password")),
                            tuple("dateOfBirth", messageService.get("validation.user.dob"))
                    );

            assertThat(userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(validSignUpRequest.getUsername(), validSignUpRequest.getEmail())).isNotPresent();
        }
    }

    @Nested
    @DisplayName("POST " + ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_LOGIN)
    @Story("User Login")
    @Severity(SeverityLevel.BLOCKER)
    class Login {

        @Test
        @DisplayName("Should return 200 and login successfully, return access token, and set refresh cookie")
        void shouldLoginSuccessfully() throws Exception {
            LoginRequestDTO loginRequest = LoginRequestDTO.builder()
                    .emailOrUsername(user.getUsername())
                    .password("TestPassword@123")
                    .build();

            MvcResult response = mockMvc.perform(post(ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_LOGIN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            LoginResponseDTO loginResponseDTO = testResponseExtractor.extractPayload(response,  LoginResponseDTO.class);
            assertThat(loginResponseDTO.getAccessToken()).isNotNull();
            assertThat(response.getResponse().getCookie("refreshToken")).isNotNull();
            assertThat(cookie().httpOnly("refreshToken", true)).isNotNull();
        }

        @Test
        @DisplayName("Should return 401 when using wrong credentials")
        void shouldReturn401ForWrongCredentials() throws Exception {
            LoginRequestDTO wrongLoginRequest = LoginRequestDTO.builder()
                    .emailOrUsername("testuser")
                    .password("WrongPassword123!")
                    .build();

            MvcResult response = mockMvc.perform(post(ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_LOGIN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(wrongLoginRequest)))
                    .andExpect(status().isUnauthorized())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo("Bad credentials");
        }

        @Test
        @DisplayName("Should return 400 when login input is null")
        void shouldReturn400WhenLoginInputIsNull() throws Exception {
            LoginRequestDTO invalidLoginRequest = LoginRequestDTO.builder()
                    .build();

            MvcResult response = mockMvc.perform(post(ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_LOGIN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidLoginRequest)))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.validation.failed"));
            assertThat(errorResponse.getSubErrors())
                    .hasSize(2)
                    .extracting(ApiError.FieldError::getField, ApiError.FieldError::getMessage)
                    .containsExactlyInAnyOrder(
                            tuple("emailOrUsername", messageService.get("validation.user.email_or_username.not_null")),
                            tuple("password", messageService.get("validation.user.password.not_null"))
                    );
        }

        @Test
        @DisplayName("Should return 400 when login input fails validation constraints")
        void shouldReturn400WhenLoginInputFailsValidationConstraints() throws Exception {
            LoginRequestDTO invalidLoginRequest = LoginRequestDTO.builder()
                    .emailOrUsername("invalid-email-format")
                    .password("short")
                    .build();

            MvcResult response = mockMvc.perform(post(ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_LOGIN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidLoginRequest)))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.validation.failed"));
            assertThat(errorResponse.getSubErrors())
                    .hasSize(2)
                    .extracting(ApiError.FieldError::getField, ApiError.FieldError::getMessage)
                    .containsExactlyInAnyOrder(
                            tuple("emailOrUsername", messageService.get("validation.user.email_or_username")),
                            tuple("password", messageService.get("validation.user.password"))
                    );
        }
    }

    @Nested
    @DisplayName("POST " + ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_REFRESH)
    @Story("Refresh Access Token")
    @Severity(SeverityLevel.BLOCKER)
    class RefreshToken {

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
            MvcResult response = mockMvc.perform(post(ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_REFRESH)
                            .cookie(validRefreshTokenCookie))
                    .andExpect(status().isOk())
                    .andReturn();

            LoginResponseDTO loginResponseDTO = testResponseExtractor.extractPayload(response, LoginResponseDTO.class);
            assertThat(loginResponseDTO.getAccessToken()).isNotNull();
            assertThat(response.getResponse().getCookie("refreshToken")).isNotNull();
        }

        @Test
        @DisplayName("Should return 401 when the mandatory refresh token cookie is missing")
        void shouldReturn401WhenCookieIsMissing() throws Exception {
            MvcResult response = mockMvc.perform(post(ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_REFRESH))
                    .andExpect(status().isUnauthorized())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo("Refresh token not found inside the Cookies");
        }
    }
}
