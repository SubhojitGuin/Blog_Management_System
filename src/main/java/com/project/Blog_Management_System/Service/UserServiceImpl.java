package com.project.Blog_Management_System.Service;

import com.project.Blog_Management_System.Dto.*;
import com.project.Blog_Management_System.Entities.FollowEntity;
import com.project.Blog_Management_System.Entities.UserEntity;
import com.project.Blog_Management_System.Exceptions.InvalidActionException;
import com.project.Blog_Management_System.Exceptions.ResourceConflictException;
import com.project.Blog_Management_System.Exceptions.ResourceNotFoundException;
import com.project.Blog_Management_System.Repositories.FollowRepository;
import com.project.Blog_Management_System.Repositories.PostRepository;
import com.project.Blog_Management_System.Repositories.UserRepository;
import com.project.Blog_Management_System.Service.Interfaces.UserService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.project.Blog_Management_System.Utils.AppUtils.getCurrentUser;
import static com.project.Blog_Management_System.Utils.ValidationUtils.isInvalidUser;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final FollowRepository followRepository;
    private final PostRepository postRepository;

    @Override
    public UserEntity getUserById(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Override
    @Transactional
    public UserEntity addUser(UserEntity user) {
        return userRepository.saveAndFlush(user);
    }

    @Override
    public UserEntity getUserByUsernameOrEmail(String username, String email) {
        return userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(username, email).orElse(null);
    }

    @Override
    @Transactional
    public ProfileUpdateDTO updateProfile(ProfileUpdateDTO profileUpdateDTO) {
        UserEntity user = getCurrentUser();
        modelMapper.map(profileUpdateDTO, user);
        userRepository.saveAndFlush(user);
        return modelMapper.map(user, ProfileUpdateDTO.class);
    }

    @Override
    @Transactional
    public void updatePassword(PasswordUpdateDTO passwordUpdateDTO) {
        UserEntity user = getCurrentUser();
        if (!passwordEncoder.matches(passwordUpdateDTO.getOldPassword(), user.getPassword())) {
            throw new BadCredentialsException("Old password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(passwordUpdateDTO.getNewPassword()));
        user.setTokenVersion(user.getTokenVersion() + 1); // Invalidate existing tokens
        userRepository.saveAndFlush(user);
    }

    @Override
    @Transactional
    public void updateUserName(UsernameUpdateDTO usernameUpdateDTO) {
        UserEntity user = getCurrentUser();
        if (userRepository.findByUsernameIgnoreCase(usernameUpdateDTO.getUsername()).isPresent()) {
            throw new ResourceConflictException("Username is already taken");
        }
        user.setUsername(usernameUpdateDTO.getUsername());
        user.setTokenVersion(user.getTokenVersion() + 1); // Invalidate existing tokens
        userRepository.saveAndFlush(user);
    }

    @Override
    public void updateEmail(EmailUpdateDTO emailUpdateDTO) {
        UserEntity user = getCurrentUser();
        if (userRepository.findByEmailIgnoreCase(emailUpdateDTO.getEmail()).isPresent()) {
            throw new ResourceConflictException("Email is already taken");
        }
        user.setEmail(emailUpdateDTO.getEmail());
        user.setTokenVersion(user.getTokenVersion() + 1); // Invalidate existing tokens
        userRepository.saveAndFlush(user);
    }

    @Override
    public UserDTO getUserProfile(String username, UUID id) {
        UserEntity user = getCurrentUser();
        UserEntity retrievedUser = userRepository.findById(id).orElse(null);

        isInvalidUser(retrievedUser, username);

        UserDTO retrievedUserDTO = modelMapper.map(retrievedUser, UserDTO.class);
        retrievedUserDTO.setIsCurrentUser(user.equals(retrievedUser));

        return retrievedUserDTO;
    }

    @Override
    public List<UserInfoDTO> searchUsers(String query) {
        return userRepository.findByUsernameContainingIgnoreCase(query, PageRequest.of(0, 10));
    }

    @Override
    @Transactional
    public void followOrUnfollowUser(String username, UUID id, FollowDTO followDTO) {
        UserEntity follower = getCurrentUser();
        UserEntity followee = userRepository.findById(id).orElse(null);

        isInvalidUser(followee, username);

        if (follower.equals(followee)) {
            throw new InvalidActionException("User cannot follow/unfollow themselves");
        }

        FollowEntity followEntity = FollowEntity.builder()
                .follower(follower)
                .following(followee)
                .build();

        if (followDTO.getFollow()) {
            if (followRepository.findByFollowerAndFollowing(follower, followee).isEmpty()) {
                followRepository.saveAndFlush(followEntity);
            }
        } else {
            if (followRepository.findByFollowerAndFollowing(follower, followee).isPresent()) {
                followRepository.deleteByFollowerAndFollowing(follower, followee);
            }
        }
    }

    @Override
    public Slice<UserInfoDTO> getFollowers(String username, UUID id, int page, int size) {
        UserEntity retrievedUser = userRepository.findById(id).orElse(null);
        isInvalidUser(retrievedUser, username);
        Pageable pageable = PageRequest.of(page, size);
        return followRepository.findFollowers(id, pageable);
    }

    @Override
    public Slice<UserInfoDTO> getFollowings(String username, UUID id, int page, int size) {
        UserEntity retrievedUser = userRepository.findById(id).orElse(null);
        isInvalidUser(retrievedUser, username);
        Pageable pageable = PageRequest.of(page, size);
        return followRepository.findFollowing(id, pageable);
    }

    @Override
    public void deleteUser() {
        UserEntity user = getCurrentUser();
        user.setActive(false);
        userRepository.saveAndFlush(user);
    }

    @Override
    public Slice<PostResponseDTO> getUserPosts(String username, UUID id, int page, int size) {
        UserEntity currentUser = getCurrentUser();
        UserEntity retrievedUser = userRepository.findById(id).orElse(null);
        isInvalidUser(retrievedUser, username);
        Pageable pageable = PageRequest.of(page, size);
        return postRepository.findPostsByUser(retrievedUser, currentUser, pageable);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsernameIgnoreCase(username).orElse(null);
    }


}
