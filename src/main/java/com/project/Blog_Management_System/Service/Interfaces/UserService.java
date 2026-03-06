package com.project.Blog_Management_System.Service.Interfaces;

import com.project.Blog_Management_System.Entities.UserEntity;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.UUID;

public interface UserService extends UserDetailsService {
    UserEntity getUserById(UUID id);

    UserEntity getUserByEmail(String email);

    UserEntity addUser(UserEntity user);

    UserEntity getUserByUsernameOrEmail(String username, String email);
}
