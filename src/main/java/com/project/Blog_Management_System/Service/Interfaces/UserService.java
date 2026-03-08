package com.project.Blog_Management_System.Service.Interfaces;

import com.project.Blog_Management_System.Dto.PasswordUpdateDTO;
import com.project.Blog_Management_System.Dto.ProfileUpdateDTO;
import com.project.Blog_Management_System.Entities.UserEntity;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.UUID;

public interface UserService extends UserDetailsService {
    UserEntity getUserById(UUID id);

    UserEntity addUser(UserEntity user);

    UserEntity getUserByUsernameOrEmail(String username, String email);

    ProfileUpdateDTO updateProfile(ProfileUpdateDTO profileUpdateDTO);

    void updatePassword(PasswordUpdateDTO passwordUpdateDTO);
}
