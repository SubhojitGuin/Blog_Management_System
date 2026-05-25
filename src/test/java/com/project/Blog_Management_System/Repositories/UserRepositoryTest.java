package com.project.Blog_Management_System.Repositories;

import com.project.Blog_Management_System.Dto.UserInfoDTO;
import com.project.Blog_Management_System.Entities.UserEntity;
import com.project.Blog_Management_System.Utils.TestEntityFactory;
import jakarta.persistence.EntityManager;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private UserEntity userOne;
    private UserEntity userTwo;
    private UserEntity userThree;

    @BeforeEach
    public void setUp() {
        userOne = saveUser("one");
        userTwo = saveUser("two");
        userThree = saveUser("three");
    }

    @Nested
    @DisplayName("findByUsernameIgnoreCase(String)")
    class FindByUsernameIgnoreCase {

        @Test
        @DisplayName("returns the user when username exists with exact case match")
        public void returnsUserWhenUsernameExistsWithExactCaseMatch() {
            Optional<UserEntity> found = userRepository.findByUsernameIgnoreCase("test-user-one");

            assertTrue(found.isPresent());
            assertEquals(userOne.getId(), found.get().getId());
            assertEquals("test-user-one", found.get().getUsername());
        }

        @Test
        @DisplayName("returns the user when username exists with different case")
        public void returnsUserWhenUsernameExistsWithDifferentCase() {
            Optional<UserEntity> found = userRepository.findByUsernameIgnoreCase("TEST-USER-ONE");

            assertTrue(found.isPresent());
            assertEquals(userOne.getId(), found.get().getId());
        }

        @Test
        @DisplayName("returns the user when username exists with mixed case")
        public void returnsUserWhenUsernameExistsWithMixedCase() {
            Optional<UserEntity> found = userRepository.findByUsernameIgnoreCase("TeSt-UsEr-OnE");

            assertTrue(found.isPresent());
            assertEquals(userOne.getId(), found.get().getId());
        }

        @Test
        @DisplayName("returns empty when username does not exist")
        public void returnsEmptyWhenUsernameDoesNotExist() {
            Optional<UserEntity> found = userRepository.findByUsernameIgnoreCase("nonexistent-username");

            assertFalse(found.isPresent());
        }
    }

    @Nested
    @DisplayName("findByUsernameIgnoreCaseOrEmailIgnoreCase(String, String)")
    class FindByUsernameIgnoreCaseOrEmailIgnoreCase {

        @Test
        @DisplayName("returns the user when username matches ignoring case")
        public void returnsUserWhenUsernameMatches() {
            Optional<UserEntity> found = userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase("TEST-USER-ONE", "other@example.com");

            assertTrue(found.isPresent());
            assertEquals(userOne.getId(), found.get().getId());
        }

        @Test
        @DisplayName("returns the user when email matches ignoring case")
        public void returnsUserWhenEmailMatches() {
            Optional<UserEntity> found = userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase("nonexistent", "TEST-USER-TWO@EXAMPLE.COM");

            assertTrue(found.isPresent());
            assertEquals(userTwo.getId(), found.get().getId());
        }

        @Test
        @DisplayName("returns the user when both username and email match but username is checked first")
        public void returnsUserWhenBothMatch() {
            Optional<UserEntity> found = userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase("TEST-USER-ONE", "TEST-USER-ONE@EXAMPLE.COM");

            assertTrue(found.isPresent());
            assertEquals(userOne.getId(), found.get().getId());
        }

        @Test
        @DisplayName("returns empty when neither username nor email matches")
        public void returnsEmptyWhenNeitherMatches() {
            Optional<UserEntity> found = userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase("nonexistent-user", "nonexistent@example.com");

            assertFalse(found.isPresent());
        }
    }

    @Nested
    @DisplayName("findByEmailIgnoreCase(String)")
    class FindByEmailIgnoreCase {

        @Test
        @DisplayName("returns the user when email exists with exact case match")
        public void returnsUserWhenEmailExistsWithExactCaseMatch() {
            Optional<UserEntity> found = userRepository.findByEmailIgnoreCase("test-user-one@example.com");

            assertTrue(found.isPresent());
            assertEquals(userOne.getId(), found.get().getId());
            assertEquals("test-user-one@example.com", found.get().getEmail());
        }

        @Test
        @DisplayName("returns the user when email exists with different case")
        public void returnsUserWhenEmailExistsWithDifferentCase() {
            Optional<UserEntity> found = userRepository.findByEmailIgnoreCase("TEST-USER-ONE@EXAMPLE.COM");

            assertTrue(found.isPresent());
            assertEquals(userOne.getId(), found.get().getId());
        }

        @Test
        @DisplayName("returns the user when email exists with mixed case")
        public void returnsUserWhenEmailExistsWithMixedCase() {
            Optional<UserEntity> found = userRepository.findByEmailIgnoreCase("TeSt-UsEr-TwO@ExAmPlE.cOm");

            assertTrue(found.isPresent());
            assertEquals(userTwo.getId(), found.get().getId());
        }

        @Test
        @DisplayName("returns empty when email does not exist")
        public void returnsEmptyWhenEmailDoesNotExist() {
            Optional<UserEntity> found = userRepository.findByEmailIgnoreCase("nonexistent@example.com");

            assertFalse(found.isPresent());
        }
    }

    @Nested
    @DisplayName("findByUsernameContainingIgnoreCaseOrNameContainingIgnoreCase(String, Pageable)")
    class FindUsersByUsernameOrNameContaining {

        @Test
        @DisplayName("returns users matching the query in username ignoring case")
        public void returnsUsersMatchingQueryInUsername() {
            List<UserInfoDTO> found = userRepository.findByUsernameContainingIgnoreCaseOrNameContainingIgnoreCase("USER-ONE", PageRequest.of(0, 10));

            assertEquals(1, found.size());
            assertEquals(userOne.getId(), found.getFirst().getId());
            assertEquals("test-user-one", found.getFirst().getUsername());
        }

        @Test
        @DisplayName("returns users matching the query in name ignoring case")
        public void returnsUsersMatchingQueryInName() {
            List<UserInfoDTO> found = userRepository.findByUsernameContainingIgnoreCaseOrNameContainingIgnoreCase("USER TWO", PageRequest.of(0, 10));

            assertEquals(1, found.size());
            assertEquals(userTwo.getId(), found.getFirst().getId());
            assertEquals("Test User two", found.getFirst().getName());
        }

        @Test
        @DisplayName("returns users matching query with partial match")
        public void returnsUsersWithPartialMatch() {
            List<UserInfoDTO> found = userRepository.findByUsernameContainingIgnoreCaseOrNameContainingIgnoreCase("USER", PageRequest.of(0, 10));

            assertEquals(3, found.size());
            assertTrue(found.stream().anyMatch(u -> u.getId().equals(userOne.getId())));
            assertTrue(found.stream().anyMatch(u -> u.getId().equals(userTwo.getId())));
            assertTrue(found.stream().anyMatch(u -> u.getId().equals(userThree.getId())));
        }

        @Test
        @DisplayName("returns users matching with case insensitive query")
        public void returnsUsersWithCaseInsensitiveQuery() {
            List<UserInfoDTO> found = userRepository.findByUsernameContainingIgnoreCaseOrNameContainingIgnoreCase("test-user-THREE", PageRequest.of(0, 10));

            assertEquals(1, found.size());
            assertEquals(userThree.getId(), found.getFirst().getId());
        }

        @Test
        @DisplayName("returns empty list when query does not match any user")
        public void returnsEmptyListWhenQueryDoesNotMatch() {
            List<UserInfoDTO> found = userRepository.findByUsernameContainingIgnoreCaseOrNameContainingIgnoreCase("nonexistent-query", PageRequest.of(0, 10));

            assertTrue(found.isEmpty());
        }

        @Test
        @DisplayName("respects the page size limit")
        public void respectsPageSizeLimit() {
            List<UserInfoDTO> found = userRepository.findByUsernameContainingIgnoreCaseOrNameContainingIgnoreCase("USER", PageRequest.of(0, 2));

            assertEquals(2, found.size());
        }

        @Test
        @DisplayName("returns correct page of results")
        public void returnsCorrectPageOfResults() {
            List<UserInfoDTO> firstPage = userRepository.findByUsernameContainingIgnoreCaseOrNameContainingIgnoreCase("USER", PageRequest.of(0, 2));
            List<UserInfoDTO> secondPage = userRepository.findByUsernameContainingIgnoreCaseOrNameContainingIgnoreCase("USER", PageRequest.of(1, 2));

            assertEquals(2, firstPage.size());
            assertEquals(1, secondPage.size());

            assertNotEquals(firstPage.getFirst().getId(), secondPage.getFirst().getId());
        }

        @Test
        @DisplayName("returns user info DTO with all required fields")
        public void returnsUserInfoDTOWithAllRequiredFields() {
            List<UserInfoDTO> found = userRepository.findByUsernameContainingIgnoreCaseOrNameContainingIgnoreCase("USER-ONE", PageRequest.of(0, 10));

            assertEquals(1, found.size());
            UserInfoDTO dto = found.getFirst();

            assertAll(
                    () -> assertNotNull(dto.getId()),
                    () -> assertNotNull(dto.getName()),
                    () -> assertNotNull(dto.getUsername()),
                    () -> assertNotNull(dto.getActive()),
                    () -> assertEquals(userOne.getId(), dto.getId()),
                    () -> assertEquals(userOne.getName(), dto.getName()),
                    () -> assertEquals(userOne.getUsername(), dto.getUsername()),
                    () -> assertTrue(dto.getActive())
            );
        }
    }

    @Nested
    @DisplayName("findInactiveUsers(LocalDateTime, Pageable)")
    class FindInactiveUsers {

        @Test
        @DisplayName("returns inactive users updated before cutoff time")
        public void returnsInactiveUsersUpdatedBeforeCutoff() {
            UserEntity inactiveUser = saveUser("inactive");
            inactiveUser.setActive(false);
            userRepository.saveAndFlush(inactiveUser);

            entityManager.flush();
            entityManager.clear();

            LocalDateTime cutoff = LocalDateTime.now();

            List<UserEntity> found = userRepository.findInactiveUsers(cutoff, PageRequest.of(0, 10)).getContent();

            assertTrue(found.stream().anyMatch(u -> u.getId().equals(inactiveUser.getId())));
        }

        @Test
        @DisplayName("does not return inactive users updated after cutoff time")
        public void doesNotReturnInactiveUsersUpdatedAfterCutoff() {
            UserEntity inactiveUser = saveUser("recently-inactive");
            inactiveUser.setActive(false);
            userRepository.saveAndFlush(inactiveUser);

            LocalDateTime cutoff = LocalDateTime.now().minusHours(1);

            List<UserEntity> found = userRepository.findInactiveUsers(cutoff, PageRequest.of(0, 10)).getContent();

            assertFalse(found.stream().anyMatch(u -> u.getId().equals(inactiveUser.getId())));
        }

        @Test
        @DisplayName("does not return active users regardless of update time")
        public void doesNotReturnActiveUsers() {
            LocalDateTime cutoff = LocalDateTime.now().plusHours(1);

            List<UserEntity> found = userRepository.findInactiveUsers(cutoff, PageRequest.of(0, 10)).getContent();

            assertFalse(found.stream().anyMatch(u -> u.getId().equals(userOne.getId())));
            assertFalse(found.stream().anyMatch(u -> u.getId().equals(userTwo.getId())));
        }

        @Test
        @DisplayName("does not return deleted users")
        public void doesNotReturnDeletedUsers() {
            UserEntity inactiveUser = saveUser("inactive-deleted");
            inactiveUser.setActive(false);
            inactiveUser.setIsDeleted(true);
            userRepository.saveAndFlush(inactiveUser);

            entityManager.flush();
            entityManager.clear();

            LocalDateTime cutoff = LocalDateTime.now();

            List<UserEntity> found = userRepository.findInactiveUsers(cutoff, PageRequest.of(0, 10)).getContent();

            assertFalse(found.stream().anyMatch(u -> u.getId().equals(inactiveUser.getId())));
        }

        @Test
        @DisplayName("returns empty list when no inactive users exist")
        public void returnsEmptyListWhenNoInactiveUsersExist() {
            LocalDateTime cutoff = LocalDateTime.now();

            List<UserEntity> found = userRepository.findInactiveUsers(cutoff, PageRequest.of(0, 10)).getContent();

            assertTrue(found.isEmpty());
        }

        @Test
        @DisplayName("returns inactive users in ascending order of update time")
        public void returnsInactiveUsersInAscendingOrderOfUpdateTime() throws InterruptedException {
            UserEntity older = saveUser("older-inactive");
            older.setActive(false);
            userRepository.saveAndFlush(older);

            entityManager.flush();
            entityManager.clear();

            Thread.sleep(100);

            UserEntity newer = saveUser("newer-inactive");
            newer.setActive(false);
            userRepository.saveAndFlush(newer);

            entityManager.flush();
            entityManager.clear();

            LocalDateTime cutoff = LocalDateTime.now();

            List<UserEntity> found = userRepository.findInactiveUsers(cutoff, PageRequest.of(0, 10)).getContent();

            assertTrue(found.size() >= 2);
            Optional<UserEntity> olderInList = found.stream().filter(u -> u.getId().equals(older.getId())).findFirst();
            Optional<UserEntity> newerInList = found.stream().filter(u -> u.getId().equals(newer.getId())).findFirst();

            if (olderInList.isPresent() && newerInList.isPresent()) {
                int olderIndex = found.indexOf(olderInList.get());
                int newerIndex = found.indexOf(newerInList.get());
                assertTrue(olderIndex < newerIndex);
            }
        }

        @Test
        @DisplayName("respects the page size limit")
        public void respectsPageSizeLimit() {
            for (int i = 0; i < 5; i++) {
                UserEntity inactiveUser = saveUser("inactive-" + i);
                inactiveUser.setActive(false);
                userRepository.saveAndFlush(inactiveUser);
            }

            entityManager.flush();
            entityManager.clear();

            LocalDateTime cutoff = LocalDateTime.now();

            List<UserEntity> found = userRepository.findInactiveUsers(cutoff, PageRequest.of(0, 3)).getContent();

            assertEquals(3, found.size());
        }
    }

    @Nested
    @DisplayName("incrementPostCount(UUID)")
    class IncrementPostCount {

        @Test
        @DisplayName("increments the post count for the requested user")
        public void incrementsPostCountForRequestedUser() {
            int initialCount = userOne.getNoOfPosts();

            int result = userRepository.incrementPostCount(userOne.getId());

            userRepository.flush();
            entityManager.clear();

            UserEntity refreshed = userRepository.findById(userOne.getId()).orElseThrow();

            assertAll(
                    () -> assertEquals(1, result),
                    () -> assertEquals(initialCount + 1, refreshed.getNoOfPosts())
            );
        }

        @Test
        @DisplayName("leaves other users' post counts unchanged")
        public void leavesOtherUsersPostCountsUnchanged() {
            int initialCountTwo = userTwo.getNoOfPosts();

            userRepository.incrementPostCount(userOne.getId());
            userRepository.flush();
            entityManager.clear();

            UserEntity refreshedTwo = userRepository.findById(userTwo.getId()).orElseThrow();

            assertEquals(initialCountTwo, refreshedTwo.getNoOfPosts());
        }

        @Test
        @DisplayName("returns zero when user does not exist")
        public void returnsZeroWhenUserDoesNotExist() {
            int result = userRepository.incrementPostCount(UUID.randomUUID());

            assertEquals(0, result);
        }

        @Test
        @DisplayName("can be called multiple times to increment count")
        public void canBeCalledMultipleTimesToIncrementCount() {
            int initialCount = userOne.getNoOfPosts();

            userRepository.incrementPostCount(userOne.getId());
            userRepository.incrementPostCount(userOne.getId());
            userRepository.incrementPostCount(userOne.getId());
            userRepository.flush();
            entityManager.clear();

            UserEntity refreshed = userRepository.findById(userOne.getId()).orElseThrow();

            assertEquals(initialCount + 3, refreshed.getNoOfPosts());
        }
    }

    @Nested
    @DisplayName("decrementPostCount(UUID)")
    class DecrementPostCount {

        @Test
        @DisplayName("decrements the post count for the requested user")
        public void decrementsPostCountForRequestedUser() {
            userOne.setNoOfPosts(5);
            userRepository.saveAndFlush(userOne);

            int result = userRepository.decrementPostCount(userOne.getId());

            userRepository.flush();
            entityManager.clear();

            UserEntity refreshed = userRepository.findById(userOne.getId()).orElseThrow();

            assertAll(
                    () -> assertEquals(1, result),
                    () -> assertEquals(4, refreshed.getNoOfPosts())
            );
        }

        @Test
        @DisplayName("leaves other users' post counts unchanged")
        public void leavesOtherUsersPostCountsUnchanged() {
            userOne.setNoOfPosts(5);
            userTwo.setNoOfPosts(3);
            userRepository.saveAndFlush(userOne);
            userRepository.saveAndFlush(userTwo);

            userRepository.decrementPostCount(userOne.getId());
            userRepository.flush();
            entityManager.clear();

            UserEntity refreshedTwo = userRepository.findById(userTwo.getId()).orElseThrow();

            assertEquals(3, refreshedTwo.getNoOfPosts());
        }

        @Test
        @DisplayName("returns zero when user does not exist")
        public void returnsZeroWhenUserDoesNotExist() {
            int result = userRepository.decrementPostCount(UUID.randomUUID());

            assertEquals(0, result);
        }

        @Test
        @DisplayName("can decrement count to zero")
        public void canDecrementCountToZero() {
            userOne.setNoOfPosts(1);
            userRepository.saveAndFlush(userOne);

            userRepository.decrementPostCount(userOne.getId());
            userRepository.flush();
            entityManager.clear();

            UserEntity refreshed = userRepository.findById(userOne.getId()).orElseThrow();

            assertEquals(0, refreshed.getNoOfPosts());
        }
    }

    @Nested
    @DisplayName("incrementFollowersCount(UUID)")
    class IncrementFollowersCount {

        @Test
        @DisplayName("increments the followers count for the requested user")
        public void incrementsFollowersCountForRequestedUser() {
            int initialCount = userOne.getNoOfFollowers();

            int result = userRepository.incrementFollowersCount(userOne.getId());

            userRepository.flush();
            entityManager.clear();

            UserEntity refreshed = userRepository.findById(userOne.getId()).orElseThrow();

            assertAll(
                    () -> assertEquals(1, result),
                    () -> assertEquals(initialCount + 1, refreshed.getNoOfFollowers())
            );
        }

        @Test
        @DisplayName("leaves other users' followers counts unchanged")
        public void leavesOtherUsersFollowersCountsUnchanged() {
            int initialCountTwo = userTwo.getNoOfFollowers();

            userRepository.incrementFollowersCount(userOne.getId());
            userRepository.flush();
            entityManager.clear();

            UserEntity refreshedTwo = userRepository.findById(userTwo.getId()).orElseThrow();

            assertEquals(initialCountTwo, refreshedTwo.getNoOfFollowers());
        }

        @Test
        @DisplayName("returns zero when user does not exist")
        public void returnsZeroWhenUserDoesNotExist() {
            int result = userRepository.incrementFollowersCount(UUID.randomUUID());

            assertEquals(0, result);
        }

        @Test
        @DisplayName("can be called multiple times to increment count")
        public void canBeCalledMultipleTimesToIncrementCount() {
            int initialCount = userOne.getNoOfFollowers();

            userRepository.incrementFollowersCount(userOne.getId());
            userRepository.incrementFollowersCount(userOne.getId());
            userRepository.incrementFollowersCount(userOne.getId());
            userRepository.flush();
            entityManager.clear();

            UserEntity refreshed = userRepository.findById(userOne.getId()).orElseThrow();

            assertEquals(initialCount + 3, refreshed.getNoOfFollowers());
        }
    }

    @Nested
    @DisplayName("decrementFollowersCount(UUID)")
    class DecrementFollowersCount {

        @Test
        @DisplayName("decrements the followers count for the requested user")
        public void decrementsFollowersCountForRequestedUser() {
            userOne.setNoOfFollowers(10);
            userRepository.saveAndFlush(userOne);

            int result = userRepository.decrementFollowersCount(userOne.getId());

            userRepository.flush();
            entityManager.clear();

            UserEntity refreshed = userRepository.findById(userOne.getId()).orElseThrow();

            assertAll(
                    () -> assertEquals(1, result),
                    () -> assertEquals(9, refreshed.getNoOfFollowers())
            );
        }

        @Test
        @DisplayName("leaves other users' followers counts unchanged")
        public void leavesOtherUsersFollowersCountsUnchanged() {
            userOne.setNoOfFollowers(10);
            userTwo.setNoOfFollowers(5);
            userRepository.saveAndFlush(userOne);
            userRepository.saveAndFlush(userTwo);

            userRepository.decrementFollowersCount(userOne.getId());
            userRepository.flush();
            entityManager.clear();

            UserEntity refreshedTwo = userRepository.findById(userTwo.getId()).orElseThrow();

            assertEquals(5, refreshedTwo.getNoOfFollowers());
        }

        @Test
        @DisplayName("returns zero when user does not exist")
        public void returnsZeroWhenUserDoesNotExist() {
            int result = userRepository.decrementFollowersCount(UUID.randomUUID());

            assertEquals(0, result);
        }

        @Test
        @DisplayName("can decrement count to zero")
        public void canDecrementCountToZero() {
            userOne.setNoOfFollowers(1);
            userRepository.saveAndFlush(userOne);

            userRepository.decrementFollowersCount(userOne.getId());
            userRepository.flush();
            entityManager.clear();

            UserEntity refreshed = userRepository.findById(userOne.getId()).orElseThrow();

            assertEquals(0, refreshed.getNoOfFollowers());
        }
    }

    @Nested
    @DisplayName("incrementFollowingsCount(UUID)")
    class IncrementFollowingsCount {

        @Test
        @DisplayName("increments the followings count for the requested user")
        public void incrementsFollowingsCountForRequestedUser() {
            int initialCount = userOne.getNoOfFollowings();

            int result = userRepository.incrementFollowingsCount(userOne.getId());

            userRepository.flush();
            entityManager.clear();

            UserEntity refreshed = userRepository.findById(userOne.getId()).orElseThrow();

            assertAll(
                    () -> assertEquals(1, result),
                    () -> assertEquals(initialCount + 1, refreshed.getNoOfFollowings())
            );
        }

        @Test
        @DisplayName("leaves other users' followings counts unchanged")
        public void leavesOtherUsersFollowingsCountsUnchanged() {
            int initialCountTwo = userTwo.getNoOfFollowings();

            userRepository.incrementFollowingsCount(userOne.getId());
            userRepository.flush();
            entityManager.clear();

            UserEntity refreshedTwo = userRepository.findById(userTwo.getId()).orElseThrow();

            assertEquals(initialCountTwo, refreshedTwo.getNoOfFollowings());
        }

        @Test
        @DisplayName("returns zero when user does not exist")
        public void returnsZeroWhenUserDoesNotExist() {
            int result = userRepository.incrementFollowingsCount(UUID.randomUUID());

            assertEquals(0, result);
        }

        @Test
        @DisplayName("can be called multiple times to increment count")
        public void canBeCalledMultipleTimesToIncrementCount() {
            int initialCount = userOne.getNoOfFollowings();

            userRepository.incrementFollowingsCount(userOne.getId());
            userRepository.incrementFollowingsCount(userOne.getId());
            userRepository.incrementFollowingsCount(userOne.getId());
            userRepository.flush();
            entityManager.clear();

            UserEntity refreshed = userRepository.findById(userOne.getId()).orElseThrow();

            assertEquals(initialCount + 3, refreshed.getNoOfFollowings());
        }
    }

    @Nested
    @DisplayName("decrementFollowingsCount(UUID)")
    class DecrementFollowingsCount {

        @Test
        @DisplayName("decrements the followings count for the requested user")
        public void decrementsFollowingsCountForRequestedUser() {
            userOne.setNoOfFollowings(8);
            userRepository.saveAndFlush(userOne);

            int result = userRepository.decrementFollowingsCount(userOne.getId());

            userRepository.flush();
            entityManager.clear();

            UserEntity refreshed = userRepository.findById(userOne.getId()).orElseThrow();

            assertAll(
                    () -> assertEquals(1, result),
                    () -> assertEquals(7, refreshed.getNoOfFollowings())
            );
        }

        @Test
        @DisplayName("leaves other users' followings counts unchanged")
        public void leavesOtherUsersFollowingsCountsUnchanged() {
            userOne.setNoOfFollowings(8);
            userTwo.setNoOfFollowings(3);
            userRepository.saveAndFlush(userOne);
            userRepository.saveAndFlush(userTwo);

            userRepository.decrementFollowingsCount(userOne.getId());
            userRepository.flush();
            entityManager.clear();

            UserEntity refreshedTwo = userRepository.findById(userTwo.getId()).orElseThrow();

            assertEquals(3, refreshedTwo.getNoOfFollowings());
        }

        @Test
        @DisplayName("returns zero when user does not exist")
        public void returnsZeroWhenUserDoesNotExist() {
            int result = userRepository.decrementFollowingsCount(UUID.randomUUID());

            assertEquals(0, result);
        }

        @Test
        @DisplayName("can decrement count to zero")
        public void canDecrementCountToZero() {
            userOne.setNoOfFollowings(1);
            userRepository.saveAndFlush(userOne);

            userRepository.decrementFollowingsCount(userOne.getId());
            userRepository.flush();
            entityManager.clear();

            UserEntity refreshed = userRepository.findById(userOne.getId()).orElseThrow();

            assertEquals(0, refreshed.getNoOfFollowings());
        }
    }

    private @NonNull UserEntity saveUser(String suffix) {
        return userRepository.saveAndFlush(TestEntityFactory.testUser(suffix));
    }
}



