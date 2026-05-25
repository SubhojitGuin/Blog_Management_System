package com.project.Blog_Management_System.Repositories;

import com.project.Blog_Management_System.Dto.LikeInfoDTO;
import com.project.Blog_Management_System.Entities.CategoryEntity;
import com.project.Blog_Management_System.Entities.LikeEntity;
import com.project.Blog_Management_System.Entities.PostEntity;
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
public class LikeRepositoryTest {

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private UserEntity user;
    private UserEntity otherUser;
    private PostEntity post;
    private PostEntity otherPost;

    @BeforeEach
    public void setUp() {
        user = saveUser("u1");
        otherUser = saveUser("u2");
        CategoryEntity category = saveCategory("c1");
        CategoryEntity otherCategory = saveCategory("c2");
        post = savePost(user, category, "p1");
        otherPost = savePost(otherUser, otherCategory, "p2");
    }

    @Nested
    @DisplayName("saveAndFlush()")
    class SaveAndFlush {

        @Test
        @DisplayName("saves a like successfully with generated id and timestamp")
        public void savesLikeSuccessfully() {
            LikeEntity saved = saveLike(user, post);

            assertNotNull(saved.getId());
            assertEquals(user, saved.getUser());
            assertEquals(post, saved.getPost());
            assertNotNull(saved.getLikedAt());
        }

        @Test
        @DisplayName("does not allow duplicate likes for the same user and post")
        public void doesNotAllowDuplicateLikesForSameUserAndPost() {
            saveLike(user, post);

            assertThrows(DataIntegrityViolationException.class, () -> saveLike(user, post));
        }
    }

    @Nested
    @DisplayName("findByUserIdAndPostId(UUID, UUID)")
    class FindByUserIdAndPostId {

        @Test
        @DisplayName("returns the like when a like exists for the given user and post")
        public void returnsLikeWhenPresent() {
            LikeEntity like = saveLike(user, post);

            Optional<LikeEntity> found = likeRepository.findByUserIdAndPostId(user.getId(), post.getId());

            assertTrue(found.isPresent());
            assertEquals(like.getId(), found.get().getId());
            assertEquals(user, found.get().getUser());
            assertEquals(post, found.get().getPost());
        }

        @Test
        @DisplayName("returns empty when the user does not match any like for the post")
        public void returnsEmptyWhenUserDoesNotMatch() {
            saveLike(user, post);

            Optional<LikeEntity> found = likeRepository.findByUserIdAndPostId(otherUser.getId(), post.getId());

            assertFalse(found.isPresent());
        }

        @Test
        @DisplayName("returns empty when the post does not match any like for the user")
        public void returnsEmptyWhenPostDoesNotMatch() {
            saveLike(user, post);

            Optional<LikeEntity> found = likeRepository.findByUserIdAndPostId(user.getId(), otherPost.getId());

            assertFalse(found.isPresent());
        }

        @Test
        @DisplayName("returns empty when neither user nor post match any like")
        public void returnsEmptyWhenNeitherMatches() {
            saveLike(user, post);

            Optional<LikeEntity> found = likeRepository.findByUserIdAndPostId(UUID.randomUUID(), UUID.randomUUID());

            assertFalse(found.isPresent());
        }
    }

    @Nested
    @DisplayName("deleteByUserIdAndPostId(UUID, UUID)")
    class DeleteByUserIdAndPostId {

        @Test
        @DisplayName("removes the matching like")
        public void removesMatchingLike() {
            saveLike(user, post);

            likeRepository.deleteByUserIdAndPostId(user.getId(), post.getId());

            Optional<LikeEntity> found = likeRepository.findByUserIdAndPostId(user.getId(), post.getId());
            assertFalse(found.isPresent());
        }

        @Test
        @DisplayName("does not throw when the like does not exist")
        public void doesNotThrowWhenLikeDoesNotExist() {
            assertDoesNotThrow(() -> {
                likeRepository.deleteByUserIdAndPostId(UUID.randomUUID(), UUID.randomUUID());
                likeRepository.flush();
            });
        }

        @Test
        @DisplayName("deletes only the matching like and leaves other likes intact")
        public void deletesOnlyMatchingLike() {
            saveLike(user, post);
            LikeEntity retained = saveLike(otherUser, post);

            likeRepository.deleteByUserIdAndPostId(user.getId(), post.getId());

            Optional<LikeEntity> deleted = likeRepository.findByUserIdAndPostId(user.getId(), post.getId());
            Optional<LikeEntity> stillPresent = likeRepository.findById(retained.getId());

            assertFalse(deleted.isPresent());
            assertTrue(stillPresent.isPresent());
            assertEquals(retained.getId(), stillPresent.get().getId());
        }
    }

    @Nested
    @DisplayName("findLikesOfPost(UUID, UUID, Pageable)")
    class FindLikesOfPost {

        @Test
        @DisplayName("returns projected likes for the requested post in descending id order")
        public void returnsProjectedLikesInDescendingOrder() {
            LikeEntity first = saveLike(user, post);
            LikeEntity second = saveLike(otherUser, post);
            UserEntity thirdUser = saveUser("u3");
            LikeEntity third = saveLike(thirdUser, post);
            saveLike(user, otherPost);

            List<LikeEntity> expected = new ArrayList<>(List.of(first, second, third));
            expected.sort(Comparator.comparing(LikeEntity::getId).reversed());

            Slice<LikeInfoDTO> slice = likeRepository.findLikesOfPost(post.getId(), null, PageRequest.of(0, 10));

            assertEquals(3, slice.getNumberOfElements());
            assertFalse(slice.hasNext());

            List<LikeInfoDTO> content = slice.getContent();
            for (int i = 0; i < content.size(); i++) {
                LikeEntity expectedLike = expected.get(i);
                LikeInfoDTO dto = content.get(i);

                assertAll(
                        () -> assertEquals(expectedLike.getId(), dto.getLikeId()),
                        () -> assertNotNull(dto.getUser()),
                        () -> assertEquals(expectedLike.getUser().getId(), dto.getUser().getId()),
                        () -> assertEquals(expectedLike.getUser().getName(), dto.getUser().getName()),
                        () -> assertEquals(expectedLike.getUser().getUsername(), dto.getUser().getUsername()),
                        () -> assertEquals(expectedLike.getUser().getActive(), dto.getUser().getActive())
                );
            }
        }

        @Test
        @DisplayName("returns only likes older than the provided cursor")
        public void returnsOnlyLikesOlderThanCursor() {
            LikeEntity first = saveLike(user, post);
            LikeEntity second = saveLike(otherUser, post);
            LikeEntity third = saveLike(saveUser("u3"), post);

            List<LikeEntity> ordered = new ArrayList<>(List.of(first, second, third));
            ordered.sort(Comparator.comparing(LikeEntity::getId).reversed());
            UUID cursor = ordered.get(1).getId();

            Slice<LikeInfoDTO> slice = likeRepository.findLikesOfPost(post.getId(), cursor, PageRequest.of(0, 10));

            assertEquals(1, slice.getNumberOfElements());
            assertFalse(slice.hasNext());
            assertTrue(slice.getContent().stream().allMatch(dto -> dto.getLikeId().compareTo(cursor) < 0));
        }

        @Test
        @DisplayName("returns hasNext when page size is smaller than total likes")
        public void returnsHasNextWhenPageSizeIsSmallerThanTotalLikes() {
            saveLike(user, post);
            saveLike(otherUser, post);
            saveLike(saveUser("u3"), post);

            Slice<LikeInfoDTO> slice = likeRepository.findLikesOfPost(post.getId(), null, PageRequest.of(0, 2));

            assertEquals(2, slice.getNumberOfElements());
            assertTrue(slice.hasNext());
        }

        @Test
        @DisplayName("returns empty slice when post has no likes")
        public void returnsEmptySliceWhenPostHasNoLikes() {
            Slice<LikeInfoDTO> slice = likeRepository.findLikesOfPost(otherPost.getId(), null, PageRequest.of(0, 10));

            assertEquals(0, slice.getNumberOfElements());
            assertFalse(slice.hasNext());
        }
    }

    private @NonNull UserEntity saveUser(String suffix) {
        return userRepository.saveAndFlush(TestEntityFactory.testUser(suffix));
    }

    private @NonNull CategoryEntity saveCategory(String suffix) {
        return categoryRepository.saveAndFlush(TestEntityFactory.testCategory(suffix));
    }

    private @NonNull PostEntity savePost(UserEntity owner, CategoryEntity category, String suffix) {
        return postRepository.saveAndFlush(TestEntityFactory.testPost(owner, category, suffix));
    }

    private @NonNull LikeEntity saveLike(UserEntity likeUser, PostEntity targetPost) {
        return likeRepository.saveAndFlush(TestEntityFactory.testLike(likeUser, targetPost));
    }
}
