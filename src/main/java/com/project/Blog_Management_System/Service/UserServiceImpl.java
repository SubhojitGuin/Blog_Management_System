package com.project.Blog_Management_System.Service;

import com.project.Blog_Management_System.Dto.*;
import com.project.Blog_Management_System.Entities.FollowEntity;
import com.project.Blog_Management_System.Entities.UserEntity;
import com.project.Blog_Management_System.Exceptions.InvalidActionException;
import com.project.Blog_Management_System.Exceptions.ResourceConflictException;
import com.project.Blog_Management_System.Exceptions.ResourceNotFoundException;
import com.project.Blog_Management_System.Repositories.BookmarkRepository;
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
    private final BookmarkRepository bookmarkRepository;

    @Override
    @Transactional(readOnly = true)
    public UserEntity getUserById(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    @Override
    @Transactional
    public UserEntity addUser(UserEntity user) {
        return userRepository.saveAndFlush(user);
    }

    @Override
    @Transactional(readOnly = true)
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
    @Transactional
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
    @Transactional(readOnly = true)
    public UserDTO getUserProfile(String username, UUID userId) {
        UserEntity user = getCurrentUser();
        UserEntity retrievedUser = userRepository.findById(userId).orElse(null);

        isInvalidUser(retrievedUser, username);

        UserDTO retrievedUserDTO = modelMapper.map(retrievedUser, UserDTO.class);
        retrievedUserDTO.setIsCurrentUser(user.equals(retrievedUser));

        return retrievedUserDTO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserInfoDTO> searchUsers(String query) {
        return userRepository.findByUsernameContainingIgnoreCaseOrNameContainingIgnoreCase(query, PageRequest.of(0, 10));
    }

    @Override
    @Transactional
    public void followOrUnfollowUser(String username, UUID userId, FollowDTO followDTO) {
        UserEntity follower = getCurrentUser();
        UserEntity followee = userRepository.findById(userId).orElse(null);

        isInvalidUser(followee, username);

        if (follower.equals(followee)) {
            throw new InvalidActionException("User cannot follow/unfollow themselves");
        }

        FollowEntity followEntity = FollowEntity.builder()
                .follower(follower)
                .following(followee)
                .build();

        if (followDTO.getFollow()) {
            if (followRepository.findByFollowerIdAndFollowingId(follower.getId(), followee.getId()).isEmpty()) {
                followRepository.saveAndFlush(followEntity);
                int followerRowsUpdated = userRepository.incrementFollowersCount(followee.getId());
                int followingsRowsUpdated = userRepository.incrementFollowingsCount(follower.getId());

                if (followerRowsUpdated == 0 || followingsRowsUpdated == 0) {
                    throw new ResourceConflictException("Failed to update followers/followings count");
                }
            }
        } else {
            if (followRepository.findByFollowerIdAndFollowingId(follower.getId(), followee.getId()).isPresent()) {
                followRepository.deleteByFollowerIdAndFollowingId(follower.getId(), followee.getId());
                int followerRowsUpdated = userRepository.decrementFollowersCount(followee.getId());
                int followingsRowsUpdated = userRepository.decrementFollowingsCount(follower.getId());

                if (followerRowsUpdated == 0 || followingsRowsUpdated == 0) {
                    throw new ResourceConflictException("Failed to update followers/followings count");
                }
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<FollowInfoDTO> getFollowers(String username, UUID userId, UUID followCursor, int size) {
        UserEntity retrievedUser = userRepository.findById(userId).orElse(null);
        isInvalidUser(retrievedUser, username);
        Pageable pageable = PageRequest.of(0, size);
        return followRepository.findFollowers(userId, followCursor, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<FollowInfoDTO> getFollowings(String username, UUID userId, UUID followCursor, int size) {
        UserEntity retrievedUser = userRepository.findById(userId).orElse(null);
        isInvalidUser(retrievedUser, username);
        Pageable pageable = PageRequest.of(0, size);
        return followRepository.findFollowing(userId, followCursor, pageable);
    }

    @Override
    @Transactional
    public void deleteUser() {
        UserEntity user = getCurrentUser();
        user.setActive(false);
        userRepository.saveAndFlush(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<PostInfoDTO> getUserPosts(String username, UUID userId, UUID postCursor, int size) {
        UserEntity retrievedUser = userRepository.findById(userId).orElse(null);
        isInvalidUser(retrievedUser, username);
        Pageable pageable = PageRequest.of(0, size);
        return postRepository.findPostsByUser(userId, postCursor, pageable);
    }

    @Override
    public Slice<BookmarkInfoDTO> getUserBookmarks(UUID bookmarkCursor, int size) {
        UserEntity user = getCurrentUser();
        Pageable pageable = PageRequest.of(0, size);
        return bookmarkRepository.findByUser(user.getId(), bookmarkCursor, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsernameIgnoreCase(username).orElse(null);
    }

}
