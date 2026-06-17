package com.project.Blog_Management_System.Security;

import com.project.Blog_Management_System.Dto.LoginRequestDTO;
import com.project.Blog_Management_System.Dto.SignUpRequestDTO;
import com.project.Blog_Management_System.Dto.UserDTO;
import com.project.Blog_Management_System.Entities.UserEntity;
import com.project.Blog_Management_System.Enums.Role;
import com.project.Blog_Management_System.Exceptions.ResourceConflictException;
import com.project.Blog_Management_System.Service.Interfaces.UserService;
import com.project.Blog_Management_System.Utils.MessageService;
import com.project.Blog_Management_System.Utils.TestEntityFactory;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JWTService jwtService;

    @Mock
    private MessageService messageService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    @Nested
    @DisplayName("signUp(SignUpRequestDTO)")
    class SignUp {

        @Test
        @DisplayName("creates a new user with encoded password and user role when username and email are available")
        void createsNewUserWhenUsernameAndEmailAreAvailable() {
            SignUpRequestDTO requestDTO = new SignUpRequestDTO();
            requestDTO.setUsername("new-user");
            requestDTO.setEmail("new-user@example.com");
            requestDTO.setPassword("plain-password");

            UserEntity mappedUser = new UserEntity();
            UserEntity savedUser = new UserEntity();
            savedUser.setId(UUID.randomUUID());
            savedUser.setUsername("new-user");
            savedUser.setEmail("new-user@example.com");
            savedUser.setRoles(Set.of(Role.USER));

            UserDTO expectedDto = new UserDTO();
            expectedDto.setUsername("new-user");

            when(userService.getUserByUsernameOrEmail("new-user", "new-user@example.com")).thenReturn(null);
            when(modelMapper.map(requestDTO, UserEntity.class)).thenReturn(mappedUser);
            when(passwordEncoder.encode("plain-password")).thenReturn("encoded-password");
            when(userService.addUser(mappedUser)).thenReturn(savedUser);
            when(modelMapper.map(savedUser, UserDTO.class)).thenReturn(expectedDto);

            UserDTO result = authService.signUp(requestDTO);

            assertNotNull(result);
            assertEquals("new-user", result.getUsername());
            assertEquals("encoded-password", mappedUser.getPassword());
            assertEquals(Set.of(Role.USER), mappedUser.getRoles());
            verify(userService).addUser(mappedUser);
        }

        @Test
        @DisplayName("throws ResourceConflictException when username or email already exists")
        void throwsWhenUsernameOrEmailAlreadyExists() {
            SignUpRequestDTO requestDTO = new SignUpRequestDTO();
            requestDTO.setUsername("existing-user");
            requestDTO.setEmail("existing@example.com");

            when(userService.getUserByUsernameOrEmail("existing-user", "existing@example.com"))
                    .thenReturn(new UserEntity());
            when(messageService.get("exception.resource.conflict", "Username/Email"))
                    .thenReturn("Username/Email already exists");

            assertThrows(ResourceConflictException.class, () -> authService.signUp(requestDTO));

            verify(userService, never()).addUser(any(UserEntity.class));
            verify(modelMapper, never()).map(eq(requestDTO), eq(UserEntity.class));
        }
    }

    @Nested
    @DisplayName("login(LoginRequestDTO)")
    class Login {

        @Test
        @DisplayName("returns access and refresh tokens when credentials are valid for an active user")
        void returnsTokensWhenCredentialsAreValidForActiveUser() {
            LoginRequestDTO requestDTO = new LoginRequestDTO();
            requestDTO.setEmailOrUsername("active-user");
            requestDTO.setPassword("secret");

            UserEntity foundUser = TestEntityFactory.testUser("active");
            foundUser.setId(UUID.randomUUID());
            foundUser.setActive(true);

            when(userService.getUserByUsernameOrEmail("active-user", "active-user"))
                    .thenReturn(foundUser);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(foundUser);
            when(jwtService.generateAccessToken(foundUser)).thenReturn("access-token");
            when(jwtService.generateRefreshToken(foundUser)).thenReturn("refresh-token");

            String[] result = authService.login(requestDTO);

            assertNotNull(result);
            assertEquals(2, result.length);
            assertEquals("access-token", result[0]);
            assertEquals("refresh-token", result[1]);
            verify(userService, never()).addUser(any(UserEntity.class));
        }

        @Test
        @DisplayName("reactivates user account on successful login when user is inactive")
        void reactivatesUserAccountWhenLoginSucceedsAndUserIsInactive() {
            LoginRequestDTO requestDTO = new LoginRequestDTO();
            requestDTO.setEmailOrUsername("inactive-user");
            requestDTO.setPassword("secret");

            UserEntity foundUser = TestEntityFactory.testUser("inactive");
            foundUser.setId(UUID.randomUUID());
            foundUser.setActive(false);

            UserEntity principalUser = TestEntityFactory.testUser("inactive");
            principalUser.setId(foundUser.getId());
            principalUser.setActive(false);

            when(userService.getUserByUsernameOrEmail("inactive-user", "inactive-user"))
                    .thenReturn(foundUser);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(principalUser);
            when(jwtService.generateAccessToken(principalUser)).thenReturn("access-token");
            when(jwtService.generateRefreshToken(principalUser)).thenReturn("refresh-token");

            String[] result = authService.login(requestDTO);

            assertEquals("access-token", result[0]);
            assertEquals("refresh-token", result[1]);
            assertTrue(foundUser.getActive());
            verify(userService).addUser(foundUser);
        }

        @Test
        @DisplayName("throws UsernameNotFoundException when no user matches email or username")
        void throwsWhenNoUserMatchesEmailOrUsername() {
            LoginRequestDTO requestDTO = new LoginRequestDTO();
            requestDTO.setEmailOrUsername("missing-user");
            requestDTO.setPassword("secret");

            when(userService.getUserByUsernameOrEmail("missing-user", "missing-user")).thenReturn(null);
            when(messageService.get("exception.auth.username_or_email_not_found"))
                    .thenReturn("Username or Email not found");

            assertThrows(UsernameNotFoundException.class, () -> authService.login(requestDTO));

            verify(authenticationManager, never()).authenticate(any());
            verify(jwtService, never()).generateAccessToken(any(UserEntity.class));
        }

        @Test
        @DisplayName("propagates authentication exception when credentials are invalid")
        void propagatesAuthenticationExceptionWhenCredentialsAreInvalid() {
            LoginRequestDTO requestDTO = new LoginRequestDTO();
            requestDTO.setEmailOrUsername("user");
            requestDTO.setPassword("wrong-password");

            UserEntity foundUser = TestEntityFactory.testUser("auth-fail");
            foundUser.setId(UUID.randomUUID());

            when(userService.getUserByUsernameOrEmail("user", "user")).thenReturn(foundUser);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThrows(BadCredentialsException.class, () -> authService.login(requestDTO));

            verify(jwtService, never()).generateAccessToken(any(UserEntity.class));
            verify(jwtService, never()).generateRefreshToken(any(UserEntity.class));
        }
    }

    @Nested
    @DisplayName("refreshToken(String)")
    class RefreshToken {

        @Test
        @DisplayName("returns new access and refresh tokens when refresh token tokenVersion matches user tokenVersion")
        void returnsNewTokensWhenTokenVersionMatches() {
            UUID userId = UUID.randomUUID();
            String refreshToken = "valid-refresh-token";

            UserEntity user = TestEntityFactory.testUser("refresh");
            user.setId(userId);
            user.setTokenVersion(2);

            when(jwtService.getUserIdFromToken(refreshToken)).thenReturn(userId);
            when(jwtService.getTokenVersionFromToken(refreshToken)).thenReturn(2);
            when(userService.getUserById(userId)).thenReturn(user);
            when(jwtService.generateAccessToken(user)).thenReturn("new-access-token");
            when(jwtService.generateRefreshToken(user)).thenReturn("new-refresh-token");

            String[] result = authService.refreshToken(refreshToken);

            assertNotNull(result);
            assertEquals(2, result.length);
            assertEquals("new-access-token", result[0]);
            assertEquals("new-refresh-token", result[1]);
        }

        @Test
        @DisplayName("throws JwtException when refresh token version does not match current user token version")
        void throwsWhenRefreshTokenVersionDoesNotMatchCurrentUserVersion() {
            UUID userId = UUID.randomUUID();
            String refreshToken = "stale-refresh-token";

            UserEntity user = TestEntityFactory.testUser("refresh-mismatch");
            user.setId(userId);
            user.setTokenVersion(4);

            when(jwtService.getUserIdFromToken(refreshToken)).thenReturn(userId);
            when(jwtService.getTokenVersionFromToken(refreshToken)).thenReturn(3);
            when(userService.getUserById(userId)).thenReturn(user);
            when(messageService.get("exception.auth.jwt_invalid_refresh_token"))
                    .thenReturn("Invalid refresh token");

            assertThrows(JwtException.class, () -> authService.refreshToken(refreshToken));

            verify(jwtService, never()).generateAccessToken(any(UserEntity.class));
            verify(jwtService, never()).generateRefreshToken(any(UserEntity.class));
        }
    }
}


