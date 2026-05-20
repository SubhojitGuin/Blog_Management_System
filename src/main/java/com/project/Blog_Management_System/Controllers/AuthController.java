package com.project.Blog_Management_System.Controllers;

import com.project.Blog_Management_System.Constants.ApiRoutes;
import com.project.Blog_Management_System.Dto.LoginRequestDTO;
import com.project.Blog_Management_System.Dto.LoginResponseDTO;
import com.project.Blog_Management_System.Dto.SignUpRequestDTO;
import com.project.Blog_Management_System.Dto.UserDTO;
import com.project.Blog_Management_System.Security.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

import static com.project.Blog_Management_System.Security.SecurityUtils.getAuthCookie;

@RestController
@RequestMapping(ApiRoutes.AUTH_BASE_PATH)
@RequiredArgsConstructor
@Tag(name = "User Authentication", description = "Authentication Operations related to users")
public class AuthController {

    private final AuthService authService;

    @PostMapping(ApiRoutes.AUTH_SIGNUP)
    @Operation(summary = "Sign up a new user", description = "Creates a new user account.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "User account created successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Username or email already exists",
                    content = @Content
            )
    })
    public ResponseEntity<UserDTO> signup(@Valid @RequestBody SignUpRequestDTO signUpRequestDto) {
        return new ResponseEntity<>(authService.signUp(signUpRequestDto), HttpStatus.CREATED);
    }

    @PostMapping(ApiRoutes.AUTH_LOGIN)
    @Operation(summary = "User login", description = "Authenticates a user and returns an JWT access token.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Success"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials",
                    content = @Content
            )
    })
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginDto, HttpServletResponse httpServletResponse) {
        String[] tokens = authService.login(loginDto);
        httpServletResponse.addCookie(getAuthCookie(tokens[1]));
        return ResponseEntity.ok(new LoginResponseDTO(tokens[0]));
    }

    @PostMapping(ApiRoutes.AUTH_REFRESH)
    @Operation(summary = "Refresh access token", description = "Generates a new access token using a refresh token.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Success"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid or Expired Refresh token",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content
            )
    })
    public ResponseEntity<LoginResponseDTO> refresh(HttpServletRequest request, HttpServletResponse httpServletResponse) {
        String refreshToken = Arrays.stream(request.getCookies())
                .filter(cookie -> "refreshToken".equals(cookie.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElseThrow(() -> new AuthenticationServiceException("Refresh token not found inside the Cookies"));

        String[] tokens = authService.refreshToken(refreshToken);
        httpServletResponse.addCookie(getAuthCookie(tokens[1]));
        return ResponseEntity.ok(new LoginResponseDTO(tokens[0]));
    }
}
