package com.project.Blog_Management_System.Controllers;

import com.project.Blog_Management_System.Constants.ApiRoutes;
import com.project.Blog_Management_System.Dto.LoginRequestDTO;
import com.project.Blog_Management_System.Dto.SignUpRequestDTO;
import com.project.Blog_Management_System.Dto.UserDTO;
import com.project.Blog_Management_System.Exceptions.ResourceConflictException;
import com.project.Blog_Management_System.Exceptions.ResourceNotFoundException;
import com.project.Blog_Management_System.Security.AuthService;
import com.project.Blog_Management_System.Security.JWTService;
import com.project.Blog_Management_System.Security.WebSecurityConfig;
import com.project.Blog_Management_System.Service.Interfaces.UserService;
import com.project.Blog_Management_System.Utils.MessageService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(WebSecurityConfig.class)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private MessageService messageService;

    @MockitoBean
    private JWTService jwtService;

    @MockitoBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private SignUpRequestDTO signUpRequestDTO;
    private LoginRequestDTO loginRequestDTO;
    private UserDTO userDTO;

    @BeforeEach
    void setUp() {
        signUpRequestDTO = new SignUpRequestDTO();
        signUpRequestDTO.setName("Test User");
        signUpRequestDTO.setUsername("testuser");
        signUpRequestDTO.setEmail("test@example.com");
        signUpRequestDTO.setPassword("Password@123");
        signUpRequestDTO.setDateOfBirth(LocalDate.of(2000, 1, 1));

        loginRequestDTO = new LoginRequestDTO();
        loginRequestDTO.setEmailOrUsername("testuser");
        loginRequestDTO.setPassword("Password@123");

        userDTO = new UserDTO();
        userDTO.setId(UUID.randomUUID());
        userDTO.setName("Test User");
        userDTO.setUsername("testuser");
    }

    @Nested
    @DisplayName("signup(SignUpRequestDTO)")
    class Signup {

        @Test
        @DisplayName("returns 201 Created when user signup is successful")
        void signupSuccessfully() throws Exception {
            when(authService.signUp(any(SignUpRequestDTO.class))).thenReturn(userDTO);

            mockMvc.perform(post(ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_SIGNUP)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(signUpRequestDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.id").value(userDTO.getId().toString()))
                    .andExpect(jsonPath("$.data.username").value(userDTO.getUsername()))
                    .andExpect(jsonPath("$.data.name").value(userDTO.getName()));
        }

        @Test
        @DisplayName("returns 409 Conflict when username already exists")
        void returnsConflictWhenUsernameExists() throws Exception {
            when(authService.signUp(any(SignUpRequestDTO.class)))
                    .thenThrow(new ResourceConflictException("Username already exists"));

            mockMvc.perform(post(ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_SIGNUP)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(signUpRequestDTO)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("returns 409 Conflict when email already exists")
        void returnsConflictWhenEmailExists() throws Exception {
            when(authService.signUp(any(SignUpRequestDTO.class)))
                    .thenThrow(new ResourceConflictException("Email already exists"));

            mockMvc.perform(post(ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_SIGNUP)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(signUpRequestDTO)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("returns 400 Bad Request when name is blank")
        void returnsBadRequestWhenNameIsBlank() throws Exception {
            signUpRequestDTO.setName("");

            mockMvc.perform(post(ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_SIGNUP)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(signUpRequestDTO)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 Bad Request when name is too short")
        void returnsBadRequestWhenNameIsTooShort() throws Exception {
            signUpRequestDTO.setName("A");

            mockMvc.perform(post(ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_SIGNUP)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(signUpRequestDTO)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 Bad Request when email is invalid")
        void returnsBadRequestWhenEmailIsInvalid() throws Exception {
            signUpRequestDTO.setEmail("invalid-email");

            mockMvc.perform(post(ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_SIGNUP)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(signUpRequestDTO)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 Bad Request when email is blank")
        void returnsBadRequestWhenEmailIsBlank() throws Exception {
            signUpRequestDTO.setEmail("");

            mockMvc.perform(post(ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_SIGNUP)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(signUpRequestDTO)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 Bad Request when password is null")
        void returnsBadRequestWhenPasswordIsNull() throws Exception {
            signUpRequestDTO.setPassword(null);

            mockMvc.perform(post(ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_SIGNUP)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(signUpRequestDTO)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 Bad Request when dateOfBirth is null")
        void returnsBadRequestWhenDateOfBirthIsNull() throws Exception {
            signUpRequestDTO.setDateOfBirth(null);

            mockMvc.perform(post(ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_SIGNUP)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(signUpRequestDTO)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 Bad Request when dateOfBirth is in future")
        void returnsBadRequestWhenDateOfBirthIsInFuture() throws Exception {
            signUpRequestDTO.setDateOfBirth(LocalDate.now().plusDays(1));

            mockMvc.perform(post(ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_SIGNUP)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(signUpRequestDTO)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("accepts valid bio when provided")
        void acceptsValidBioWhenProvided() throws Exception {
            signUpRequestDTO.setBio("This is my bio");
            when(authService.signUp(any(SignUpRequestDTO.class))).thenReturn(userDTO);

            mockMvc.perform(post(ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_SIGNUP)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(signUpRequestDTO)))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("login(LoginRequestDTO, HttpServletResponse)")
    class Login {

        @Test
        @DisplayName("returns 200 OK with access token when login is successful")
        void loginSuccessfully() throws Exception {
            String accessToken = "access-token-xyz";
            String refreshToken = "refresh-token-abc";
            String[] tokens = {accessToken, refreshToken};

            when(authService.login(any(LoginRequestDTO.class))).thenReturn(tokens);

            mockMvc.perform(post(ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_LOGIN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequestDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").value(accessToken));
        }

        @Test
        @DisplayName("sets refresh token cookie in response")
        void setsRefreshTokenCookie() throws Exception {
            String accessToken = "access-token-xyz";
            String refreshToken = "refresh-token-abc";
            String[] tokens = {accessToken, refreshToken};

            when(authService.login(any(LoginRequestDTO.class))).thenReturn(tokens);

            mockMvc.perform(post(ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_LOGIN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequestDTO)))
                    .andExpect(status().isOk())
                    .andExpect(cookie().exists("refreshToken"));
        }

        @Test
        @DisplayName("returns 401 Unauthorized with invalid credentials")
        void returnsUnauthorizedWithInvalidCredentials() throws Exception {
            when(authService.login(any(LoginRequestDTO.class)))
                    .thenThrow(new UsernameNotFoundException("Invalid credentials"));

            mockMvc.perform(post(ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_LOGIN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequestDTO)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("returns 400 Bad Request with missing password")
        void returnsBadRequestWithMissingPassword() throws Exception {
            LoginRequestDTO invalidLoginDTO = new LoginRequestDTO();
            invalidLoginDTO.setEmailOrUsername("testuser");

            mockMvc.perform(post(ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_LOGIN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidLoginDTO)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 Bad Request when emailOrUsername is null")
        void returnsBadRequestWhenEmailOrUsernameIsNull() throws Exception {
            LoginRequestDTO invalidLoginDTO = new LoginRequestDTO();
            invalidLoginDTO.setEmailOrUsername(null);
            invalidLoginDTO.setPassword("Password@123");

            mockMvc.perform(post(ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_LOGIN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidLoginDTO)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 Bad Request when password is null")
        void returnsBadRequestWhenPasswordIsNull() throws Exception {
            LoginRequestDTO invalidLoginDTO = new LoginRequestDTO();
            invalidLoginDTO.setEmailOrUsername("testuser");
            invalidLoginDTO.setPassword(null);

            mockMvc.perform(post(ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_LOGIN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidLoginDTO)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("accepts valid email as emailOrUsername")
        void acceptsValidEmailAsEmailOrUsername() throws Exception {
            String accessToken = "access-token";
            String refreshToken = "refresh-token";
            String[] tokens = {accessToken, refreshToken};

            LoginRequestDTO validLoginDTO = new LoginRequestDTO();
            validLoginDTO.setEmailOrUsername("user@example.com");
            validLoginDTO.setPassword("Password@123");

            when(authService.login(any(LoginRequestDTO.class))).thenReturn(tokens);

            mockMvc.perform(post(ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_LOGIN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validLoginDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").value(accessToken));
        }
    }

    @Nested
    @DisplayName("refresh(HttpServletRequest, HttpServletResponse)")
    class Refresh {

        @Test
        @DisplayName("returns 200 OK with new access token when refresh is successful")
        void refreshSuccessfully() throws Exception {
            String newAccessToken = "new-access-token";
            String newRefreshToken = "new-refresh-token";
            String[] tokens = {newAccessToken, newRefreshToken};

            when(authService.refreshToken("valid-refresh-token")).thenReturn(tokens);

            mockMvc.perform(post(ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_REFRESH)
                            .cookie(new Cookie("refreshToken", "valid-refresh-token")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").value(newAccessToken));
        }

        @Test
        @DisplayName("sets new refresh token cookie in response")
        void setsNewRefreshTokenCookie() throws Exception {
            String newAccessToken = "new-access-token";
            String newRefreshToken = "new-refresh-token";
            String[] tokens = {newAccessToken, newRefreshToken};

            when(authService.refreshToken("valid-refresh-token")).thenReturn(tokens);

            mockMvc.perform(post(ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_REFRESH)
                            .cookie(new Cookie("refreshToken", "valid-refresh-token")))
                    .andExpect(status().isOk())
                    .andExpect(cookie().exists("refreshToken"));
        }

        @Test
        @DisplayName("returns 401 Unauthorized when refresh token is invalid")
        void returnsUnauthorizedWithInvalidRefreshToken() throws Exception {
            when(authService.refreshToken("invalid-refresh-token"))
                    .thenThrow(new AuthenticationServiceException("Invalid or Expired Refresh token"));

            mockMvc.perform(post(ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_REFRESH)
                            .cookie(new Cookie("refreshToken", "invalid-refresh-token")))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("returns 401 Unauthorized when refresh token is expired")
        void returnsUnauthorizedWithExpiredRefreshToken() throws Exception {
            when(authService.refreshToken("expired-refresh-token"))
                    .thenThrow(new AuthenticationServiceException("Refresh token expired"));

            mockMvc.perform(post(ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_REFRESH)
                            .cookie(new Cookie("refreshToken", "expired-refresh-token")))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("returns 401 Unauthorized when refresh token cookie is missing")
        void returnsBadRequestWhenRefreshTokenCookieMissing() throws Exception {
            mockMvc.perform(post(ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_REFRESH))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("returns 404 Not Found when user not found")
        void returnsNotFoundWhenUserNotFound() throws Exception {
            when(authService.refreshToken("valid-refresh-token"))
                    .thenThrow(new ResourceNotFoundException("User not found"));

            mockMvc.perform(post(ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_REFRESH)
                            .cookie(new Cookie("refreshToken", "valid-refresh-token")))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("returns 401 Unauthorized with empty refresh token")
        void returnsUnauthorizedWithEmptyRefreshToken() throws Exception {
            when(authService.refreshToken(""))
                    .thenThrow(new AuthenticationServiceException("Invalid token"));

            mockMvc.perform(post(ApiRoutes.AUTH_BASE_PATH + ApiRoutes.AUTH_REFRESH)
                            .cookie(new Cookie("refreshToken", "")))
                    .andExpect(status().isUnauthorized());
        }
    }
}

