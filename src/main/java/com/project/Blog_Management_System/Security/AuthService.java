package com.project.Blog_Management_System.Security;

import com.project.Blog_Management_System.Dto.LoginRequestDTO;
import com.project.Blog_Management_System.Dto.SignUpRequestDTO;
import com.project.Blog_Management_System.Dto.UserDTO;
import com.project.Blog_Management_System.Entities.UserEntity;
import com.project.Blog_Management_System.Enums.Role;
import com.project.Blog_Management_System.Exceptions.ResourceConflictException;
import com.project.Blog_Management_System.Service.Interfaces.UserService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;

    public UserDTO signUp(SignUpRequestDTO signUpRequestDto) {

        UserEntity user = userService.getUserByUsernameOrEmail(signUpRequestDto.getUsername(), signUpRequestDto.getEmail());
        if (user != null) {
            throw new ResourceConflictException("User is already present with same email id or username");
        }

        UserEntity newUser = modelMapper.map(signUpRequestDto, UserEntity.class);
        newUser.setRoles(Set.of(Role.USER));
        newUser.setPassword(passwordEncoder.encode(signUpRequestDto.getPassword()));
        newUser = userService.addUser(newUser);

        return modelMapper.map(newUser, UserDTO.class);
    }

    public String[] login(LoginRequestDTO loginRequestDTO) {
        String username = loginRequestDTO.getEmailOrUsername();

        UserEntity loginUser = userService.getUserByUsernameOrEmail(username, username);
        if (loginUser == null) {
            throw new UsernameNotFoundException("User not found with email or username: " + username);
        }
        username = loginUser.getUsername();

        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                username, loginRequestDTO.getPassword()
        ));

        UserEntity user = (UserEntity) authentication.getPrincipal();

        String[] arr = new String[2];
        arr[0] = jwtService.generateAccessToken(user);
        arr[1] = jwtService.generateRefreshToken(user);

        if (!user.getActive()) {
            loginUser.setActive(true);
            userService.addUser(loginUser);
        }

        return arr;
    }

    public String refreshToken(String refreshToken) {
        UUID id = jwtService.getUserIdFromToken(refreshToken);
        int tokenVersion = jwtService.getTokenVersionFromToken(refreshToken);
        UserEntity user = userService.getUserById(id);

        if (!user.getTokenVersion().equals(tokenVersion)) {
            throw new JwtException("Invalid refresh token");
        }

        return jwtService.generateAccessToken(user);
    }

}
