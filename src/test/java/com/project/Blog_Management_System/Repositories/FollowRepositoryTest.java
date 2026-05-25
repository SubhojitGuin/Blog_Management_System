package com.project.Blog_Management_System.Repositories;

import com.project.Blog_Management_System.Dto.FollowInfoDTO;
import com.project.Blog_Management_System.Entities.FollowEntity;
import com.project.Blog_Management_System.Entities.UserEntity;
import com.project.Blog_Management_System.Utils.TestEntityFactory;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
public class FollowRepositoryTest {

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private UserRepository userRepository;

    private UserEntity followerOne;
    private UserEntity followerTwo;
    private UserEntity followerThree;
    private UserEntity followingUser;
    private UserEntity anotherFollowingUser;

    @BeforeEach
    public void setUp() {
        followerOne = saveUser("f1");
        followerTwo = saveUser("f2");
        followerThree = saveUser("f3");
        followingUser = saveUser("follow");
        anotherFollowingUser = saveUser("follow2");
    }

    @Nested
    @DisplayName("saveAndFlush()")
    class SaveAndFlush {

        @Test
        @DisplayName("saves a follow relationship successfully with a generated id")
        public void savesFollowSuccessfully() {
            FollowEntity follow = saveFollow(followerOne, followingUser);

            assertNotNull(follow.getId());
            assertEquals(followerOne, follow.getFollower());
            assertEquals(followingUser, follow.getFollowing());
            assertNotNull(follow.getFollowedAt());
        }

        @Test
        @DisplayName("does not allow duplicate follower and following pairs")
        public void preventsDuplicateFollowerAndFollowingPair() {
            saveFollow(followerOne, followingUser);

            assertThrows(DataIntegrityViolationException.class, () -> saveFollow(followerOne, followingUser));
        }

        @Test
        @DisplayName("allows the opposite follow direction as a distinct relationship")
        public void allowsOppositeDirectionAsDistinctFollow() {
            FollowEntity forward = saveFollow(followerOne, followingUser);
            FollowEntity reverse = saveFollow(followingUser, followerOne);

            assertNotNull(forward.getId());
            assertNotNull(reverse.getId());
            assertNotEquals(forward.getId(), reverse.getId());
        }
    }

    @Nested
    @DisplayName("findFollowers(UUID, UUID, Pageable)")
    class FindFollowers {

        @Test
        @DisplayName("returns projected followers for the requested user in descending id order")
        public void returnsProjectedFollowersInDescendingOrder() {
            FollowEntity first = saveFollow(followerOne, followingUser);
            FollowEntity second = saveFollow(followerTwo, followingUser);
            FollowEntity third = saveFollow(followerThree, followingUser);
            saveFollow(followerOne, anotherFollowingUser);

            List<FollowEntity> expected = new ArrayList<>(List.of(first, second, third));
            expected.sort(Comparator.comparing(FollowEntity::getId).reversed());

            Slice<FollowInfoDTO> slice = followRepository.findFollowers(
                    followingUser.getId(),
                    null,
                    PageRequest.of(0, 10)
            );

            assertEquals(3, slice.getNumberOfElements());
            assertFalse(slice.hasNext());

            List<FollowInfoDTO> content = slice.getContent();
            for (int i = 0; i < content.size(); i++) {
                FollowEntity expectedFollow = expected.get(i);
                FollowInfoDTO dto = content.get(i);

                assertAll(
                        () -> assertEquals(expectedFollow.getId(), dto.getFollowId()),
                        () -> assertNotNull(dto.getUser()),
                        () -> assertEquals(expectedFollow.getFollower().getId(), dto.getUser().getId()),
                        () -> assertEquals(expectedFollow.getFollower().getName(), dto.getUser().getName()),
                        () -> assertEquals(expectedFollow.getFollower().getUsername(), dto.getUser().getUsername()),
                        () -> assertEquals(expectedFollow.getFollower().getActive(), dto.getUser().getActive())
                );
            }
        }

        @Test
        @DisplayName("returns only followers older than the provided cursor")
        public void returnsOnlyFollowersOlderThanCursor() {
            FollowEntity first = saveFollow(followerOne, followingUser);
            FollowEntity second = saveFollow(followerTwo, followingUser);
            FollowEntity third = saveFollow(followerThree, followingUser);

            List<FollowEntity> ordered = new ArrayList<>(List.of(first, second, third));
            ordered.sort(Comparator.comparing(FollowEntity::getId).reversed());
            UUID cursor = ordered.get(1).getId();

            Slice<FollowInfoDTO> slice = followRepository.findFollowers(
                    followingUser.getId(),
                    cursor,
                    PageRequest.of(0, 10)
            );

            assertEquals(1, slice.getNumberOfElements());
            assertFalse(slice.hasNext());
            assertTrue(slice.getContent().stream().allMatch(dto -> dto.getFollowId().compareTo(cursor) < 0));
        }

        @Test
        @DisplayName("returns hasNext when the page size is smaller than the number of followers")
        public void returnsHasNextWhenPageSizeIsSmallerThanResultSize() {
            saveFollow(followerOne, followingUser);
            saveFollow(followerTwo, followingUser);
            saveFollow(followerThree, followingUser);

            Slice<FollowInfoDTO> slice = followRepository.findFollowers(
                    followingUser.getId(),
                    null,
                    PageRequest.of(0, 2)
            );

            assertEquals(2, slice.getNumberOfElements());
            assertTrue(slice.hasNext());
        }

        @Test
        @DisplayName("returns an empty slice when the user has no followers")
        public void returnsEmptySliceWhenUserHasNoFollowers() {
            Slice<FollowInfoDTO> slice = followRepository.findFollowers(
                    anotherFollowingUser.getId(),
                    null,
                    PageRequest.of(0, 10)
            );

            assertEquals(0, slice.getNumberOfElements());
            assertFalse(slice.hasNext());
        }
    }

    @Nested
    @DisplayName("findFollowing(UUID, UUID, Pageable)")
    class FindFollowing {

        @Test
        @DisplayName("returns projected followings for the requested user in descending id order")
        public void returnsProjectedFollowingsInDescendingOrder() {
            FollowEntity first = saveFollow(followerOne, followingUser);
            FollowEntity second = saveFollow(followerOne, anotherFollowingUser);
            FollowEntity third = saveFollow(followerOne, saveUser("f4"));
            saveFollow(followerTwo, followingUser);

            List<FollowEntity> expected = new ArrayList<>(List.of(first, second, third));
            expected.sort(Comparator.comparing(FollowEntity::getId).reversed());

            Slice<FollowInfoDTO> slice = followRepository.findFollowing(
                    followerOne.getId(),
                    null,
                    PageRequest.of(0, 10)
            );

            assertEquals(3, slice.getNumberOfElements());
            assertFalse(slice.hasNext());

            List<FollowInfoDTO> content = slice.getContent();
            for (int i = 0; i < content.size(); i++) {
                FollowEntity expectedFollow = expected.get(i);
                FollowInfoDTO dto = content.get(i);

                assertAll(
                        () -> assertEquals(expectedFollow.getId(), dto.getFollowId()),
                        () -> assertNotNull(dto.getUser()),
                        () -> assertEquals(expectedFollow.getFollowing().getId(), dto.getUser().getId()),
                        () -> assertEquals(expectedFollow.getFollowing().getName(), dto.getUser().getName()),
                        () -> assertEquals(expectedFollow.getFollowing().getUsername(), dto.getUser().getUsername()),
                        () -> assertEquals(expectedFollow.getFollowing().getActive(), dto.getUser().getActive())
                );
            }
        }

        @Test
        @DisplayName("returns only followings older than the provided cursor")
        public void returnsOnlyFollowingsOlderThanCursor() {
            FollowEntity first = saveFollow(followerOne, followingUser);
            FollowEntity second = saveFollow(followerOne, anotherFollowingUser);
            FollowEntity third = saveFollow(followerOne, saveUser("f4"));

            List<FollowEntity> ordered = new ArrayList<>(List.of(first, second, third));
            ordered.sort(Comparator.comparing(FollowEntity::getId).reversed());
            UUID cursor = ordered.get(1).getId();

            Slice<FollowInfoDTO> slice = followRepository.findFollowing(
                    followerOne.getId(),
                    cursor,
                    PageRequest.of(0, 10)
            );

            assertEquals(1, slice.getNumberOfElements());
            assertFalse(slice.hasNext());
            assertTrue(slice.getContent().stream().allMatch(dto -> dto.getFollowId().compareTo(cursor) < 0));
        }

        @Test
        @DisplayName("returns hasNext when the page size is smaller than the number of followings")
        public void returnsHasNextWhenPageSizeIsSmallerThanResultSize() {
            saveFollow(followerOne, followingUser);
            saveFollow(followerOne, anotherFollowingUser);
            saveFollow(followerOne, saveUser("f4"));

            Slice<FollowInfoDTO> slice = followRepository.findFollowing(
                    followerOne.getId(),
                    null,
                    PageRequest.of(0, 2)
            );

            assertEquals(2, slice.getNumberOfElements());
            assertTrue(slice.hasNext());
        }

        @Test
        @DisplayName("returns an empty slice when the user follows nobody")
        public void returnsEmptySliceWhenUserFollowsNobody() {
            Slice<FollowInfoDTO> slice = followRepository.findFollowing(
                    anotherFollowingUser.getId(),
                    null,
                    PageRequest.of(0, 10)
            );

            assertEquals(0, slice.getNumberOfElements());
            assertFalse(slice.hasNext());
        }
    }

    @Nested
    @DisplayName("deleteByFollowerIdAndFollowingId(UUID, UUID)")
    class DeleteByFollowerIdAndFollowingId {

        @Test
        @DisplayName("removes the matching follow relationship")
        public void removesMatchingFollow() {
            saveFollow(followerOne, followingUser);

            followRepository.deleteByFollowerIdAndFollowingId(followerOne.getId(), followingUser.getId());

            Optional<FollowEntity> found = followRepository.findByFollowerIdAndFollowingId(followerOne.getId(), followingUser.getId());
            assertFalse(found.isPresent());
        }

        @Test
        @DisplayName("does not throw when the follow relationship does not exist")
        public void doesNotThrowWhenFollowDoesNotExist() {
            assertDoesNotThrow(() -> {
                followRepository.deleteByFollowerIdAndFollowingId(UUID.randomUUID(), UUID.randomUUID());
                followRepository.flush();
            });
        }

        @Test
        @DisplayName("deletes only the matching follow and leaves other follows intact")
        public void deletesOnlyMatchingFollow() {
            saveFollow(followerOne, followingUser);
            FollowEntity retained = saveFollow(followerTwo, followingUser);

            followRepository.deleteByFollowerIdAndFollowingId(followerOne.getId(), followingUser.getId());

            Optional<FollowEntity> deleted = followRepository.findByFollowerIdAndFollowingId(followerOne.getId(), followingUser.getId());
            Optional<FollowEntity> remaining = followRepository.findById(retained.getId());

            assertFalse(deleted.isPresent());
            assertTrue(remaining.isPresent());
            assertEquals(retained.getId(), remaining.get().getId());
        }
    }

    @Nested
    @DisplayName("deleteByFollowerIdOrFollowingId(UUID, UUID)")
    class DeleteByFollowerIdOrFollowingId {

        @Test
        @DisplayName("removes all follows matching either the follower or the following id")
        public void removesFollowsMatchingEitherId() {
            saveFollow(followerOne, followingUser);
            saveFollow(followerOne, anotherFollowingUser);
            saveFollow(followerTwo, followingUser);
            FollowEntity unrelated = saveFollow(followerThree, anotherFollowingUser);

            followRepository.deleteByFollowerIdOrFollowingId(followerOne.getId(), followingUser.getId());

            assertFalse(followRepository.findByFollowerIdAndFollowingId(followerOne.getId(), followingUser.getId()).isPresent());
            assertFalse(followRepository.findByFollowerIdAndFollowingId(followerOne.getId(), anotherFollowingUser.getId()).isPresent());
            assertFalse(followRepository.findByFollowerIdAndFollowingId(followerTwo.getId(), followingUser.getId()).isPresent());
            assertTrue(followRepository.findById(unrelated.getId()).isPresent());
        }

        @Test
        @DisplayName("does not throw when no follow relationships match")
        public void doesNotThrowWhenNoFollowRelationshipsMatch() {
            assertDoesNotThrow(() -> {
                followRepository.deleteByFollowerIdOrFollowingId(UUID.randomUUID(), UUID.randomUUID());
                followRepository.flush();
            });
        }

        @Test
        @DisplayName("leaves unrelated follow relationships intact")
        public void leavesUnrelatedFollowRelationshipsIntact() {
            FollowEntity retained = saveFollow(followerThree, anotherFollowingUser);

            followRepository.deleteByFollowerIdOrFollowingId(followerOne.getId(), followingUser.getId());

            assertTrue(followRepository.findById(retained.getId()).isPresent());
        }
    }

    @Nested
    @DisplayName("findByFollowerIdAndFollowingId(UUID, UUID)")
    class FindByFollowerIdAndFollowingId {

        @Test
        @DisplayName("returns the follow when the follower and following match an existing relationship")
        public void returnsFollowWhenPresent() {
            FollowEntity follow = saveFollow(followerOne, followingUser);

            Optional<FollowEntity> found = followRepository.findByFollowerIdAndFollowingId(followerOne.getId(), followingUser.getId());

            assertTrue(found.isPresent());
            assertEquals(follow.getId(), found.get().getId());
            assertEquals(followerOne, found.get().getFollower());
            assertEquals(followingUser, found.get().getFollowing());
        }

        @Test
        @DisplayName("returns empty when the follower does not match any follow relationship")
        public void returnsEmptyWhenFollowerDoesNotMatch() {
            saveFollow(followerOne, followingUser);

            Optional<FollowEntity> found = followRepository.findByFollowerIdAndFollowingId(followerTwo.getId(), followingUser.getId());

            assertFalse(found.isPresent());
        }

        @Test
        @DisplayName("returns empty when the following does not match any follow relationship")
        public void returnsEmptyWhenFollowingDoesNotMatch() {
            saveFollow(followerOne, followingUser);

            Optional<FollowEntity> found = followRepository.findByFollowerIdAndFollowingId(followerOne.getId(), anotherFollowingUser.getId());

            assertFalse(found.isPresent());
        }

        @Test
        @DisplayName("returns empty when neither the follower nor the following match any follow relationship")
        public void returnsEmptyWhenNeitherIdMatches() {
            saveFollow(followerOne, followingUser);

            Optional<FollowEntity> found = followRepository.findByFollowerIdAndFollowingId(UUID.randomUUID(), UUID.randomUUID());

            assertFalse(found.isPresent());
        }
    }

    @Nested
    @DisplayName("findByFollowingId(UUID)")
    class FindByFollowingId {

        @Test
        @DisplayName("returns all follow relationships for the requested following user")
        public void returnsAllFollowsForFollowingUser() {
            FollowEntity first = saveFollow(followerOne, followingUser);
            FollowEntity second = saveFollow(followerTwo, followingUser);
            saveFollow(followerThree, anotherFollowingUser);

            List<FollowEntity> follows = followRepository.findByFollowingId(followingUser.getId());

            assertEquals(2, follows.size());
            assertTrue(follows.stream().anyMatch(follow -> follow.getId().equals(first.getId())));
            assertTrue(follows.stream().anyMatch(follow -> follow.getId().equals(second.getId())));
        }

        @Test
        @DisplayName("returns an empty list when the user has no followers")
        public void returnsEmptyListWhenFollowingUserHasNoFollowers() {
            List<FollowEntity> follows = followRepository.findByFollowingId(anotherFollowingUser.getId());

            assertTrue(follows.isEmpty());
        }
    }

    @Nested
    @DisplayName("findById(UUID)")
    class FindById {

        @Test
        @DisplayName("returns the follow when the id exists")
        public void returnsFollowWhenIdExists() {
            FollowEntity follow = saveFollow(followerOne, followingUser);

            Optional<FollowEntity> found = followRepository.findById(follow.getId());

            assertTrue(found.isPresent());
            assertEquals(follow.getId(), found.get().getId());
            assertEquals(followerOne, found.get().getFollower());
            assertEquals(followingUser, found.get().getFollowing());
        }

        @Test
        @DisplayName("returns empty when no follow exists with the given id")
        public void returnsEmptyWhenIdDoesNotExist() {
            Optional<FollowEntity> found = followRepository.findById(UUID.randomUUID());

            assertFalse(found.isPresent());
        }
    }

    @Nested
    @DisplayName("findAll()")
    class FindAll {

        @Test
        @DisplayName("returns all follow relationships when multiple exist")
        public void returnsAllFollowRelationshipsWhenMultipleExist() {
            FollowEntity first = saveFollow(followerOne, followingUser);
            FollowEntity second = saveFollow(followerTwo, followingUser);
            FollowEntity third = saveFollow(followerThree, anotherFollowingUser);

            List<FollowEntity> allFollows = followRepository.findAll();

            assertEquals(3, allFollows.size());
            assertTrue(allFollows.stream().anyMatch(follow -> follow.getId().equals(first.getId())));
            assertTrue(allFollows.stream().anyMatch(follow -> follow.getId().equals(second.getId())));
            assertTrue(allFollows.stream().anyMatch(follow -> follow.getId().equals(third.getId())));
        }

        @Test
        @DisplayName("returns an empty list when no follow relationships exist")
        public void returnsEmptyListWhenNoFollowRelationshipsExist() {
            followRepository.deleteAll();

            List<FollowEntity> allFollows = followRepository.findAll();

            assertTrue(allFollows.isEmpty());
        }
    }

    private @NonNull UserEntity saveUser(String suffix) {
        return userRepository.saveAndFlush(TestEntityFactory.testUser(suffix));
    }

    private @NonNull FollowEntity saveFollow(UserEntity follower, UserEntity following) {
        return followRepository.saveAndFlush(TestEntityFactory.testFollow(follower, following));
    }
}
