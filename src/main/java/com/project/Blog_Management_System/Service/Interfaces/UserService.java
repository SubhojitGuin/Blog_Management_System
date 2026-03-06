package com.project.Blog_Management_System.Service.Interfaces;

import com.project.Blog_Management_System.Entities.UserEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.UUID;

public interface UserService {
    UserEntity getUserById(UUID id);

    UserEntity getUserByEmail(String email);

    UserEntity addUser(UserEntity user);

    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;
}
