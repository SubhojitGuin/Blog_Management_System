package com.project.Blog_Management_System.Service.Interfaces;

import com.project.Blog_Management_System.Dto.*;
import com.project.Blog_Management_System.Entities.UserEntity;
import org.springframework.data.domain.Slice;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;
import java.util.UUID;

public interface UserService extends UserDetailsService {

    UserEntity getUserById(UUID userId);

    UserEntity addUser(UserEntity user);

    UserEntity getUserByUsernameOrEmail(String username, String email);

    ProfileUpdateDTO updateProfile(ProfileUpdateDTO profileUpdateDTO);

    void updatePassword(PasswordUpdateDTO passwordUpdateDTO);

    void updateUserName(UsernameUpdateDTO usernameUpdateDTO);

    void updateEmail(EmailUpdateDTO emailUpdateDTO);

    UserDTO getUserProfile(String username, UUID userId);

    List<UserInfoDTO> searchUsers(String query);

    void followOrUnfollowUser(String username, UUID userId, FollowDTO followDTO);

    Slice<UserInfoDTO> getFollowers(String username, UUID userId, int page, int size);

    Slice<UserInfoDTO> getFollowings(String username, UUID userId, int page, int size);

    void deleteUser();

    Slice<PostResponseDTO> getUserPosts(String username, UUID userId, int page, int size);
}
