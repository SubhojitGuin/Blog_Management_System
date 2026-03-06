package com.project.Blog_Management_System.Security;

import com.project.Blog_Management_System.Dto.LoginRequestDTO;
import com.project.Blog_Management_System.Dto.SignUpRequestDTO;
import com.project.Blog_Management_System.Dto.UserDTO;
import com.project.Blog_Management_System.Entities.UserEntity;
import com.project.Blog_Management_System.Enums.Role;
import com.project.Blog_Management_System.Exceptions.ResourceConflictException;
import com.project.Blog_Management_System.Service.Interfaces.UserService;
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

        UserEntity user = userService.getUserByEmail(signUpRequestDto.getEmail());
        if (user == null) {
            user = (UserEntity) userService.loadUserByUsername(signUpRequestDto.getUsername());
        }

        if (user != null) {
            throw new ResourceConflictException("User is already present with same email id or username");
        }

        UserEntity newUser = modelMapper.map(signUpRequestDto, UserEntity.class);
        newUser.setRoles(Set.of(Role.USER));
        newUser.setPassword(passwordEncoder.encode(signUpRequestDto.getPassword()));
        newUser.setActive(true);
        newUser = userService.addUser(newUser);

        return modelMapper.map(newUser, UserDTO.class);
    }

    public String[] login(LoginRequestDTO loginRequestDTO) {
        String username = loginRequestDTO.getEmailOrUsername();

        if (username.contains("@")) {
            UserEntity user = userService.getUserByEmail(username);
            if (user == null) {
                throw new UsernameNotFoundException("User not found with email: " + username);
            }
        }

        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                username, loginRequestDTO.getPassword()
        ));

        UserEntity user = (UserEntity) authentication.getPrincipal();

        String[] arr = new String[2];
        arr[0] = jwtService.generateAccessToken(user);
        arr[1] = jwtService.generateRefreshToken(user);

        return arr;
    }

    public String refreshToken(String refreshToken) {
        UUID id = jwtService.getUserIdFromToken(refreshToken);

        UserEntity user = userService.getUserById(id);
        return jwtService.generateAccessToken(user);
    }

}
