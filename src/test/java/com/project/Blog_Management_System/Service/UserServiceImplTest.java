package com.project.Blog_Management_System.Service;

import com.project.Blog_Management_System.Dto.*;
import com.project.Blog_Management_System.Entities.FollowEntity;
import com.project.Blog_Management_System.Entities.UserEntity;
import com.project.Blog_Management_System.Enums.Gender;
import com.project.Blog_Management_System.Events.NewFollowerEvent;
import com.project.Blog_Management_System.Exceptions.InvalidActionException;
import com.project.Blog_Management_System.Exceptions.ResourceConflictException;
import com.project.Blog_Management_System.Exceptions.ResourceNotFoundException;
import com.project.Blog_Management_System.Repositories.FollowRepository;
import com.project.Blog_Management_System.Repositories.UserRepository;
import com.project.Blog_Management_System.Utils.MessageService;
import com.project.Blog_Management_System.Utils.TestEntityFactory;
import com.project.Blog_Management_System.Utils.ValidationUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private FollowRepository followRepository;

    @Mock
    private com.project.Blog_Management_System.Repositories.PostRepository postRepository;

    @Mock
    private com.project.Blog_Management_System.Repositories.BookmarkRepository bookmarkRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ValidationUtils validationUtils;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private UserServiceImpl userService;

    private UserEntity currentUser;

    @BeforeEach
    void setUp() {
        currentUser = TestEntityFactory.testUser("current");
        currentUser.setId(UUID.randomUUID());

        currentUser.setTokenVersion(1);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(currentUser, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("addUser(UserEntity)")
    class AddUser {

        @Test
        @DisplayName("returns the saved user when a new user is added")
        void returnsSavedUserWhenUserIsAdded() {
            UserEntity newUser = TestEntityFactory.testUser("new");
            newUser.setId(UUID.randomUUID());

            when(userRepository.saveAndFlush(newUser)).thenReturn(newUser);

            UserEntity result = userService.addUser(newUser);

            assertSame(newUser, result);
            verify(userRepository).saveAndFlush(newUser);
        }
    }

    @Nested
    @DisplayName("getUserByUsernameOrEmail(String, String)")
    class GetUserByUsernameOrEmail {

        @Test
        @DisplayName("returns the matching user when username or email exists")
        void returnsMatchingUserWhenUsernameOrEmailExists() {
            String username = currentUser.getUsername();
            String email = currentUser.getEmail();

            when(userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(username, email))
                    .thenReturn(Optional.of(currentUser));

            UserEntity result = userService.getUserByUsernameOrEmail(username, email);

            assertSame(currentUser, result);
            verify(userRepository).findByUsernameIgnoreCaseOrEmailIgnoreCase(username, email);
        }

        @Test
        @DisplayName("returns null when no user matches username or email")
        void returnsNullWhenNoUserMatches() {
            String username = "missing-user";
            String email = "missing@example.com";

            when(userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(username, email))
                    .thenReturn(Optional.empty());

            UserEntity result = userService.getUserByUsernameOrEmail(username, email);

            assertNull(result);
            verify(userRepository).findByUsernameIgnoreCaseOrEmailIgnoreCase(username, email);
        }
    }

    @Nested
    @DisplayName("updateProfile(ProfileUpdateDTO)")
    class UpdateProfile {

        @Test
        @DisplayName("updates the current user's profile and returns the refreshed profile data")
        void updatesCurrentUserProfileAndReturnsMappedDto() {
            ProfileUpdateDTO dto = new ProfileUpdateDTO();
            dto.setName("Updated Name");
            dto.setBio("Updated bio");
            dto.setGender(Gender.FEMALE);
            dto.setDateOfBirth(LocalDate.of(1998, 5, 20));

            doAnswer(invocation -> {
                ProfileUpdateDTO source = invocation.getArgument(0);
                UserEntity target = invocation.getArgument(1);
                target.setName(source.getName());
                target.setBio(source.getBio());
                target.setGender(source.getGender());
                target.setDateOfBirth(source.getDateOfBirth());
                return null;
            }).when(modelMapper).map(dto, currentUser);

            when(userRepository.saveAndFlush(currentUser)).thenReturn(currentUser);

            ProfileUpdateDTO mappedResponse = new ProfileUpdateDTO();
            mappedResponse.setName(dto.getName());
            mappedResponse.setBio(dto.getBio());
            mappedResponse.setGender(dto.getGender());
            mappedResponse.setDateOfBirth(dto.getDateOfBirth());
            when(modelMapper.map(currentUser, ProfileUpdateDTO.class)).thenReturn(mappedResponse);

            ProfileUpdateDTO result = userService.updateProfile(dto);

            assertNotNull(result);
            assertEquals(dto.getName(), result.getName());
            assertEquals(dto.getBio(), result.getBio());
            assertEquals(dto.getGender(), result.getGender());
            assertEquals(dto.getDateOfBirth(), result.getDateOfBirth());
            assertEquals(dto.getName(), currentUser.getName());
            assertEquals(dto.getBio(), currentUser.getBio());
            assertEquals(dto.getGender(), currentUser.getGender());
            assertEquals(dto.getDateOfBirth(), currentUser.getDateOfBirth());

            verify(modelMapper).map(dto, currentUser);
            verify(userRepository).saveAndFlush(currentUser);
            verify(modelMapper).map(currentUser, ProfileUpdateDTO.class);
        }
    }

    @Nested
    @DisplayName("updatePassword(PasswordUpdateDTO)")
    class UpdatePassword {

        @Test
        @DisplayName("updates password and increments token version when old password matches")
        void updatesPasswordAndInvalidatesTokensWhenOldMatches() {
            PasswordUpdateDTO dto = new PasswordUpdateDTO();
            dto.setOldPassword("old-pass");
            dto.setNewPassword("new-pass");

            when(passwordEncoder.matches("old-pass", currentUser.getPassword())).thenReturn(true);
            when(passwordEncoder.encode("new-pass")).thenReturn("encoded-new-pass");
            when(userRepository.saveAndFlush(any(UserEntity.class))).thenAnswer(i -> i.getArgument(0));

            userService.updatePassword(dto);

            ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
            verify(userRepository).saveAndFlush(captor.capture());

            UserEntity saved = captor.getValue();
            assertEquals("encoded-new-pass", saved.getPassword());
            assertEquals(2, saved.getTokenVersion());
        }

        @Test
        @DisplayName("throws BadCredentialsException when old password does not match")
        void throwsWhenOldPasswordDoesNotMatch() {
            PasswordUpdateDTO dto = new PasswordUpdateDTO();
            dto.setOldPassword("wrong-old");
            dto.setNewPassword("new-pass");

            when(passwordEncoder.matches("wrong-old", currentUser.getPassword())).thenReturn(false);

            assertThrows(BadCredentialsException.class, () -> userService.updatePassword(dto));

            verify(userRepository, never()).saveAndFlush(any());
        }
    }

    @Nested
    @DisplayName("updateUserName(UsernameUpdateDTO)")
    class UpdateUserName {

        @Test
        @DisplayName("updates username when new username is available and invalidates tokens")
        void updatesUsernameWhenAvailable() {
            UsernameUpdateDTO dto = new UsernameUpdateDTO();
            dto.setUsername("newUsername");

            when(userRepository.findByUsernameIgnoreCase("newUsername")).thenReturn(Optional.empty());
            when(userRepository.saveAndFlush(any(UserEntity.class))).thenAnswer(i -> i.getArgument(0));

            userService.updateUserName(dto);

            ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
            verify(userRepository).saveAndFlush(captor.capture());
            UserEntity saved = captor.getValue();
            assertEquals("newUsername", saved.getUsername());
            assertEquals(2, saved.getTokenVersion());
        }

        @Test
        @DisplayName("throws ResourceConflictException when username already exists")
        void throwsWhenUsernameTaken() {
            UsernameUpdateDTO dto = new UsernameUpdateDTO();
            dto.setUsername("existingUser");

            when(userRepository.findByUsernameIgnoreCase("existingUser")).thenReturn(Optional.of(new UserEntity()));

            assertThrows(ResourceConflictException.class, () -> userService.updateUserName(dto));

            verify(userRepository).findByUsernameIgnoreCase("existingUser");
            verify(userRepository, never()).saveAndFlush(any());
        }
    }

    @Nested
    @DisplayName("updateEmail(EmailUpdateDTO)")
    class UpdateEmail {

        @Test
        @DisplayName("updates email when new email is available and invalidates tokens")
        void updatesEmailWhenAvailable() {
            EmailUpdateDTO dto = new EmailUpdateDTO();
            dto.setEmail("new@example.com");

            when(userRepository.findByEmailIgnoreCase("new@example.com")).thenReturn(Optional.empty());
            when(userRepository.saveAndFlush(any(UserEntity.class))).thenAnswer(i -> i.getArgument(0));

            userService.updateEmail(dto);

            ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
            verify(userRepository).saveAndFlush(captor.capture());
            UserEntity saved = captor.getValue();
            assertEquals("new@example.com", saved.getEmail());
            assertEquals(2, saved.getTokenVersion());
        }

        @Test
        @DisplayName("throws ResourceConflictException when email already exists")
        void throwsWhenEmailTaken() {
            EmailUpdateDTO dto = new EmailUpdateDTO();
            dto.setEmail("exists@example.com");

            when(userRepository.findByEmailIgnoreCase("exists@example.com")).thenReturn(Optional.of(new UserEntity()));

            assertThrows(ResourceConflictException.class, () -> userService.updateEmail(dto));

            verify(userRepository).findByEmailIgnoreCase("exists@example.com");
            verify(userRepository, never()).saveAndFlush(any());
        }
    }

    @Nested
    @DisplayName("followOrUnfollowUser(String, UUID, FollowDTO)")
    class FollowOrUnfollow {

        @Test
        @DisplayName("follows a user, updates counts and publishes NewFollowerEvent when not already following")
        void followsUserAndPublishesEventWhenNotAlreadyFollowing() {
            UserEntity followee = new UserEntity();
            followee.setId(UUID.randomUUID());
            followee.setName("Followee");
            followee.setEmail("followee@example.com");
            followee.setUsername("followeeUser");

            when(userRepository.findById(followee.getId())).thenReturn(Optional.of(followee));
            when(followRepository.findByFollowerIdAndFollowingId(currentUser.getId(), followee.getId())).thenReturn(Optional.empty());
            when(userRepository.incrementFollowersCount(followee.getId())).thenReturn(1);
            when(userRepository.incrementFollowingsCount(currentUser.getId())).thenReturn(1);
            doNothing().when(eventPublisher).publishEvent(any(NewFollowerEvent.class));

            FollowDTO dto = new FollowDTO();
            dto.setFollow(true);

            userService.followOrUnfollowUser(followee.getUsername(), followee.getId(), dto);

            verify(followRepository).saveAndFlush(any(FollowEntity.class));
            verify(userRepository).incrementFollowersCount(followee.getId());
            verify(userRepository).incrementFollowingsCount(currentUser.getId());
            verify(eventPublisher).publishEvent(any(NewFollowerEvent.class));
        }

        @Test
        @DisplayName("unfollows a user, updates counts when following relationship exists")
        void unfollowsUserAndUpdatesCountsWhenFollowingExists() {
            UserEntity followee = new UserEntity();
            followee.setId(UUID.randomUUID());
            followee.setUsername("followeeUser");

            when(userRepository.findById(followee.getId())).thenReturn(Optional.of(followee));
            when(followRepository.findByFollowerIdAndFollowingId(currentUser.getId(), followee.getId())).thenReturn(Optional.of(new FollowEntity()));
            doNothing().when(followRepository).deleteByFollowerIdAndFollowingId(currentUser.getId(), followee.getId());
            when(userRepository.decrementFollowersCount(followee.getId())).thenReturn(1);
            when(userRepository.decrementFollowingsCount(currentUser.getId())).thenReturn(1);

            FollowDTO dto = new FollowDTO();
            dto.setFollow(false);

            userService.followOrUnfollowUser(followee.getUsername(), followee.getId(), dto);

            verify(followRepository).deleteByFollowerIdAndFollowingId(currentUser.getId(), followee.getId());
            verify(userRepository).decrementFollowersCount(followee.getId());
            verify(userRepository).decrementFollowingsCount(currentUser.getId());
        }

        @Test
        @DisplayName("throws InvalidActionException when user tries to follow themself")
        void throwsWhenFollowingSelf() {
            FollowDTO dto = new FollowDTO();
            dto.setFollow(true);

            when(userRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));

            assertThrows(InvalidActionException.class, () -> userService.followOrUnfollowUser(currentUser.getUsername(), currentUser.getId(), dto));
        }

                @Test
        @DisplayName("throws ResourceConflictException when incrementing followee followers count fails during follow action")
        void throwsResourceConflictWhenIncrementFollowersCountFails() {
            UserEntity followee = new UserEntity();
            followee.setId(UUID.randomUUID());
            followee.setUsername("followeeUser");

            when(userRepository.findById(followee.getId())).thenReturn(Optional.of(followee));
            when(followRepository.findByFollowerIdAndFollowingId(currentUser.getId(), followee.getId())).thenReturn(Optional.empty());

            // Simulating failure (0 rows updated) for followee's followers count increment
            when(userRepository.incrementFollowersCount(followee.getId())).thenReturn(0);
            when(messageService.get(eq("exception.resource.conflict.count_update_failure"), any(), any(), any()))
                    .thenReturn("Resource conflict exception message");

            FollowDTO dto = new FollowDTO();
            dto.setFollow(true);

            assertThrows(ResourceConflictException.class, () ->
                userService.followOrUnfollowUser(followee.getUsername(), followee.getId(), dto)
            );

            verify(followRepository).saveAndFlush(any(FollowEntity.class));
            verify(userRepository).incrementFollowersCount(followee.getId());
            verify(eventPublisher, never()).publishEvent(any(NewFollowerEvent.class));
        }

        @Test
        @DisplayName("throws ResourceConflictException when incrementing follower followings count fails during follow action")
        void throwsResourceConflictWhenIncrementFollowingsCountFails() {
            UserEntity followee = new UserEntity();
            followee.setId(UUID.randomUUID());
            followee.setUsername("followeeUser");

            when(userRepository.findById(followee.getId())).thenReturn(Optional.of(followee));
            when(followRepository.findByFollowerIdAndFollowingId(currentUser.getId(), followee.getId())).thenReturn(Optional.empty());

            // Followers count increments cleanly, but followings count fails
            when(userRepository.incrementFollowersCount(followee.getId())).thenReturn(1);
            when(userRepository.incrementFollowingsCount(currentUser.getId())).thenReturn(0);
            when(messageService.get(eq("exception.resource.conflict.count_update_failure"), any(), any(), any()))
                    .thenReturn("Resource conflict exception message");

            FollowDTO dto = new FollowDTO();
            dto.setFollow(true);

            assertThrows(ResourceConflictException.class, () ->
                userService.followOrUnfollowUser(followee.getUsername(), followee.getId(), dto)
            );

            verify(followRepository).saveAndFlush(any(FollowEntity.class));
            verify(userRepository).incrementFollowersCount(followee.getId());
            verify(userRepository).incrementFollowingsCount(currentUser.getId());
            verify(eventPublisher, never()).publishEvent(any(NewFollowerEvent.class));
        }

        @Test
        @DisplayName("throws ResourceConflictException when decrementing followee followers count fails during unfollow action")
        void throwsResourceConflictWhenDecrementFollowersCountFails() {
            UserEntity followee = new UserEntity();
            followee.setId(UUID.randomUUID());
            followee.setUsername("followeeUser");

            when(userRepository.findById(followee.getId())).thenReturn(Optional.of(followee));
            when(followRepository.findByFollowerIdAndFollowingId(currentUser.getId(), followee.getId())).thenReturn(Optional.of(new FollowEntity()));

            // Simulating failure (0 rows updated) for followee's followers count decrement
            when(userRepository.decrementFollowersCount(followee.getId())).thenReturn(0);
            when(messageService.get(eq("exception.resource.conflict.count_update_failure"), any(), any(), any()))
                    .thenReturn("Resource conflict exception message");

            FollowDTO dto = new FollowDTO();
            dto.setFollow(false);

            assertThrows(ResourceConflictException.class, () ->
                userService.followOrUnfollowUser(followee.getUsername(), followee.getId(), dto)
            );

            verify(followRepository).deleteByFollowerIdAndFollowingId(currentUser.getId(), followee.getId());
            verify(userRepository).decrementFollowersCount(followee.getId());
        }

        @Test
        @DisplayName("throws ResourceConflictException when decrementing follower followings count fails during unfollow action")
        void throwsResourceConflictWhenDecrementFollowingsCountFails() {
            UserEntity followee = new UserEntity();
            followee.setId(UUID.randomUUID());
            followee.setUsername("followeeUser");

            when(userRepository.findById(followee.getId())).thenReturn(Optional.of(followee));
            when(followRepository.findByFollowerIdAndFollowingId(currentUser.getId(), followee.getId())).thenReturn(Optional.of(new FollowEntity()));

            // Followers count decrements cleanly, but followings count fails
            when(userRepository.decrementFollowersCount(followee.getId())).thenReturn(1);
            when(userRepository.decrementFollowingsCount(currentUser.getId())).thenReturn(0);
            when(messageService.get(eq("exception.resource.conflict.count_update_failure"), any(), any(), any()))
                    .thenReturn("Resource conflict exception message");

            FollowDTO dto = new FollowDTO();
            dto.setFollow(false);

            assertThrows(ResourceConflictException.class, () ->
                userService.followOrUnfollowUser(followee.getUsername(), followee.getId(), dto)
            );

            verify(followRepository).deleteByFollowerIdAndFollowingId(currentUser.getId(), followee.getId());
            verify(userRepository).decrementFollowersCount(followee.getId());
            verify(userRepository).decrementFollowingsCount(currentUser.getId());
        }
    }

    @Nested
    @DisplayName("getUserProfile(String, UUID)")
    class GetUserProfile {

        @Test
        @DisplayName("returns UserDTO with isCurrentUser true when requested profile is current user")
        void returnsProfileMarkedAsCurrentUser() {
            when(userRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));

            UserDTO mapped = new UserDTO();
            when(modelMapper.map(currentUser, UserDTO.class)).thenReturn(mapped);

            UserDTO result = userService.getUserProfile(currentUser.getUsername(), currentUser.getId());

            assertNotNull(result);
            assertTrue(result.getIsCurrentUser());
            verify(validationUtils).isInvalidUser(currentUser, currentUser.getUsername());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when target user does not exist")
        void throwsWhenUserMissing() {
            UUID missing = UUID.randomUUID();
            when(userRepository.findById(missing)).thenReturn(Optional.empty());
            doThrow(new ResourceNotFoundException("not found")).when(validationUtils).isInvalidUser(null, "missingUser");

            assertThrows(ResourceNotFoundException.class, () -> userService.getUserProfile("missingUser", missing));
        }
    }

    @Nested
    @DisplayName("searchUsers(String)")
    class SearchUsers {

        @Test
        @DisplayName("returns a list of UserInfoDTO matching query")
        void returnsListOfMatchingUsers() {
            List<UserInfoDTO> mockList = List.of(new UserInfoDTO(), new UserInfoDTO());
            when(userRepository.findByUsernameContainingIgnoreCaseOrNameContainingIgnoreCase(eq("john"), any(PageRequest.class))).thenReturn(mockList);

            List<UserInfoDTO> result = userService.searchUsers("john");

            assertEquals(2, result.size());
            verify(userRepository).findByUsernameContainingIgnoreCaseOrNameContainingIgnoreCase(eq("john"), any(PageRequest.class));
        }
    }

    @Nested
    @DisplayName("getUserById(UUID)")
    class GetUserById {

        @Test
        @DisplayName("returns user entity when found by id")
        void returnsUserWhenFound() {
            when(userRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
            UserEntity result = userService.getUserById(currentUser.getId());
            assertSame(currentUser, result);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when user id not found")
        void throwsWhenNotFound() {
            UUID missing = UUID.randomUUID();
            when(userRepository.findById(missing)).thenReturn(Optional.empty());
            when(messageService.get("exception.resource.not_found", "User")).thenReturn("not found");

            assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(missing));
        }
    }

    @Nested
    @DisplayName("getFollowers(String, UUID, UUID, int)")
    class GetFollowers {

        @Test
        @DisplayName("returns a slice of followers for an existing user")
        void returnsSliceOfFollowersForExistingUser() {
            UUID userId = UUID.randomUUID();
            UserEntity target = new UserEntity();
            target.setId(userId);
            target.setUsername("targetUser");

            Slice<FollowInfoDTO> mockSlice = mock(Slice.class);

            when(userRepository.findById(userId)).thenReturn(Optional.of(target));
            when(followRepository.findFollowers(eq(userId), eq(null), any(PageRequest.class))).thenReturn(mockSlice);

            Slice<FollowInfoDTO> result = userService.getFollowers(target.getUsername(), userId, null, 10);

            assertSame(mockSlice, result);
            verify(validationUtils).isInvalidUser(target, target.getUsername());
            verify(followRepository).findFollowers(eq(userId), eq(null), any(PageRequest.class));
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when target user does not exist")
        void throwsWhenTargetUserMissing() {
            UUID missing = UUID.randomUUID();
            when(userRepository.findById(missing)).thenReturn(Optional.empty());
            doThrow(new ResourceNotFoundException("not found")).when(validationUtils).isInvalidUser(null, "missingUser");

            assertThrows(ResourceNotFoundException.class, () -> userService.getFollowers("missingUser", missing, null, 5));
        }
    }

    @Nested
    @DisplayName("getFollowings(String, UUID, UUID, int)")
    class GetFollowings {

        @Test
        @DisplayName("returns a slice of followings for an existing user")
        void returnsSliceOfFollowingsForExistingUser() {
            UUID userId = UUID.randomUUID();
            UserEntity target = new UserEntity();
            target.setId(userId);
            target.setUsername("targetUser");

            Slice<FollowInfoDTO> mockSlice = mock(Slice.class);

            when(userRepository.findById(userId)).thenReturn(Optional.of(target));
            when(followRepository.findFollowing(eq(userId), eq(null), any(PageRequest.class))).thenReturn(mockSlice);

            Slice<FollowInfoDTO> result = userService.getFollowings(target.getUsername(), userId, null, 8);

            assertSame(mockSlice, result);
            verify(validationUtils).isInvalidUser(target, target.getUsername());
            verify(followRepository).findFollowing(eq(userId), eq(null), any(PageRequest.class));
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when target user is missing")
        void throwsWhenTargetMissing() {
            UUID missing = UUID.randomUUID();
            when(userRepository.findById(missing)).thenReturn(Optional.empty());
            doThrow(new ResourceNotFoundException("not found")).when(validationUtils).isInvalidUser(null, "missingUser");

            assertThrows(ResourceNotFoundException.class, () -> userService.getFollowings("missingUser", missing, null, 3));
        }
    }

    @Nested
    @DisplayName("deleteUser()")
    class DeleteUser {

        @Test
        @DisplayName("marks the current user as inactive and saves the change")
        void marksCurrentUserInactiveAndSaves() {
            when(userRepository.saveAndFlush(any(UserEntity.class))).thenAnswer(i -> i.getArgument(0));

            userService.deleteUser();

            ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
            verify(userRepository).saveAndFlush(captor.capture());
            UserEntity saved = captor.getValue();
            assertFalse(saved.getActive());
        }
    }

    @Nested
    @DisplayName("getUserPosts(String, UUID, UUID, int)")
    class GetUserPosts {

        @Test
        @DisplayName("returns a slice of posts for an existing user")
        void returnsSliceOfPostsForExistingUser() {
            UUID userId = UUID.randomUUID();
            UserEntity target = new UserEntity();
            target.setId(userId);
            target.setUsername("poster");

            Slice<PostInfoDTO> mockSlice = mock(Slice.class);

            when(userRepository.findById(userId)).thenReturn(Optional.of(target));
            when(postRepository.findPostsByUser(eq(userId), eq(null), any(PageRequest.class))).thenReturn(mockSlice);

            Slice<PostInfoDTO> result = userService.getUserPosts(target.getUsername(), userId, null, 6);

            assertSame(mockSlice, result);
            verify(validationUtils).isInvalidUser(target, target.getUsername());
            verify(postRepository).findPostsByUser(eq(userId), eq(null), any(PageRequest.class));
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when target user does not exist")
        void throwsWhenUserMissing() {
            UUID missing = UUID.randomUUID();
            when(userRepository.findById(missing)).thenReturn(Optional.empty());
            doThrow(new ResourceNotFoundException("not found")).when(validationUtils).isInvalidUser(null, "missingUser");

            assertThrows(ResourceNotFoundException.class, () -> userService.getUserPosts("missingUser", missing, null, 2));
        }
    }

    @Nested
    @DisplayName("getUserBookmarks(UUID, int)")
    class GetUserBookmarks {

        @Test
        @DisplayName("returns a slice of bookmarks for the current user")
        void returnsSliceOfBookmarksForCurrentUser() {
            Slice<BookmarkInfoDTO> mockSlice = mock(Slice.class);

            when(bookmarkRepository.findByUser(eq(currentUser.getId()), eq(null), any(PageRequest.class))).thenReturn(mockSlice);

            Slice<BookmarkInfoDTO> result = userService.getUserBookmarks(null, 7);

            assertSame(mockSlice, result);
            verify(bookmarkRepository).findByUser(eq(currentUser.getId()), eq(null), any(PageRequest.class));
        }
    }

    @Nested
    @DisplayName("loadUserByUsername(String)")
    class LoadUserByUsername {

        @Test
        @DisplayName("returns UserDetails when a user with the username exists")
        void returnsUserDetailsWhenUserExists() {
            UserEntity found = TestEntityFactory.testUser("found");
            when(userRepository.findByUsernameIgnoreCase("foundUser")).thenReturn(Optional.of(found));

            UserDetails result = userService.loadUserByUsername("foundUser");

            assertSame(found, result);
            verify(userRepository).findByUsernameIgnoreCase("foundUser");
        }

        @Test
        @DisplayName("returns null when no user with the username exists")
        void returnsNullWhenUserNotFound() {
            when(userRepository.findByUsernameIgnoreCase("missing")).thenReturn(Optional.empty());

            UserDetails result = userService.loadUserByUsername("missing");

            assertNull(result);
        }
    }
}

