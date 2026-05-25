package com.project.Blog_Management_System.Repositories;

import com.project.Blog_Management_System.Dto.PostInfoDTO;
import com.project.Blog_Management_System.Dto.PostResponseDTO;
import com.project.Blog_Management_System.Entities.CategoryEntity;
import com.project.Blog_Management_System.Entities.PostEntity;
import com.project.Blog_Management_System.Entities.UserEntity;
import com.project.Blog_Management_System.Enums.PostStatus;
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
import org.springframework.data.domain.Slice;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
public class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private EntityManager entityManager;

    private UserEntity currentUser;
    private UserEntity authorOne;
    private UserEntity authorTwo;
    private UserEntity stranger;
    private CategoryEntity categoryOne;
    private CategoryEntity categoryTwo;
    private CategoryEntity categoryThree;

    @BeforeEach
    public void setUp() {
        currentUser = saveUser("current");
        authorOne = saveUser("author-1");
        authorTwo = saveUser("author-2");
        stranger = saveUser("stranger");
        categoryOne = saveCategory("one");
        categoryTwo = saveCategory("two");
        categoryThree = saveCategory("three");
    }

    @Nested
    @DisplayName("findPostsByUser(UUID, UUID, Pageable)")
    class FindPostsByUser {

        @Test
        @DisplayName("returns posts for the requested user in descending id order")
        public void returnsPostsForRequestedUserInDescendingOrder() {
            PostEntity first = savePost(authorOne, categoryOne, "first", PostStatus.PUBLISHED, null);
            PostEntity second = savePost(authorOne, categoryTwo, "second", PostStatus.DRAFT, null);
            PostEntity third = savePost(authorOne, categoryOne, "third", PostStatus.PUBLISHED, null);
            savePost(authorTwo, categoryOne, "ignored", PostStatus.PUBLISHED, null);

            List<PostEntity> expected = new ArrayList<>(List.of(first, second, third));
            expected.sort(Comparator.comparing(PostEntity::getId).reversed());

            Slice<PostInfoDTO> slice = postRepository.findPostsByUser(authorOne.getId(), null, PageRequest.of(0, 10));

            assertEquals(3, slice.getNumberOfElements());
            assertFalse(slice.hasNext());

            List<PostInfoDTO> content = slice.getContent();
            for (int i = 0; i < content.size(); i++) {
                PostEntity expectedPost = expected.get(i);
                PostInfoDTO dto = content.get(i);

                assertAll(
                        () -> assertEquals(expectedPost.getId(), dto.getId()),
                        () -> assertEquals(expectedPost.getSlug(), dto.getSlug()),
                        () -> assertEquals(expectedPost.getTitle(), dto.getTitle()),
                        () -> assertEquals(expectedPost.getDescription(), dto.getDescription()),
                        () -> assertEquals(expectedPost.getReadingTimeMinutes(), dto.getReadingTimeMinutes()),
                        () -> assertEquals(expectedPost.getLikeCount(), dto.getLikeCount()),
                        () -> assertEquals(expectedPost.getCommentCount(), dto.getCommentCount()),
                        () -> assertEquals(expectedPost.getViewCount(), dto.getViewCount())
                );
            }
        }

        @Test
        @DisplayName("returns only posts older than the provided cursor")
        public void returnsOnlyPostsOlderThanCursor() {
            PostEntity first = savePost(authorOne, categoryOne, "first", PostStatus.PUBLISHED, null);
            PostEntity second = savePost(authorOne, categoryOne, "second", PostStatus.DRAFT, null);
            PostEntity third = savePost(authorOne, categoryOne, "third", PostStatus.PUBLISHED, null);

            List<PostEntity> ordered = new ArrayList<>(List.of(first, second, third));
            ordered.sort(Comparator.comparing(PostEntity::getId).reversed());
            UUID cursor = ordered.get(1).getId();

            Slice<PostInfoDTO> slice = postRepository.findPostsByUser(authorOne.getId(), cursor, PageRequest.of(0, 10));

            assertEquals(1, slice.getNumberOfElements());
            assertFalse(slice.hasNext());
            assertTrue(slice.getContent().stream().allMatch(dto -> dto.getId().compareTo(cursor) < 0));
        }

        @Test
        @DisplayName("returns an empty slice when the user has no posts")
        public void returnsEmptySliceWhenUserHasNoPosts() {
            Slice<PostInfoDTO> slice = postRepository.findPostsByUser(currentUser.getId(), null, PageRequest.of(0, 10));

            assertEquals(0, slice.getNumberOfElements());
            assertFalse(slice.hasNext());
        }
    }

    @Nested
    @DisplayName("findPostsByCategory(UUID, UUID, UUID, Pageable)")
    class FindPostsByCategory {

        @Test
        @DisplayName("returns projected posts for the requested category in descending id order")
        public void returnsProjectedPostsForRequestedCategoryInDescendingOrder() {
            PostEntity ownedAndLiked = savePost(currentUser, categoryOne, "owned-liked", PostStatus.PUBLISHED, null);
            PostEntity followedAndLiked = savePost(authorOne, categoryOne, "followed-liked", PostStatus.PUBLISHED, null);
            PostEntity unrelatedOwner = savePost(authorTwo, categoryOne, "unliked", PostStatus.PUBLISHED, null);
            savePost(authorOne, categoryTwo, "other-category", PostStatus.PUBLISHED, null);

            saveLike(currentUser, ownedAndLiked);
            saveLike(currentUser, followedAndLiked);

            List<PostEntity> expected = new ArrayList<>(List.of(ownedAndLiked, followedAndLiked, unrelatedOwner));
            expected.sort(Comparator.comparing(PostEntity::getId).reversed());

            Slice<PostResponseDTO> slice = postRepository.findPostsByCategory(
                    categoryOne.getId(),
                    null,
                    currentUser.getId(),
                    PageRequest.of(0, 10)
            );

            assertEquals(3, slice.getNumberOfElements());
            assertFalse(slice.hasNext());

            List<PostResponseDTO> content = slice.getContent();
            for (int i = 0; i < content.size(); i++) {
                PostEntity expectedPost = expected.get(i);
                PostResponseDTO dto = content.get(i);

                assertAll(
                        () -> assertEquals(expectedPost.getId(), dto.getId()),
                        () -> assertEquals(expectedPost.getSlug(), dto.getSlug()),
                        () -> assertEquals(expectedPost.getTitle(), dto.getTitle()),
                        () -> assertEquals(expectedPost.getDescription(), dto.getDescription()),
                        () -> assertEquals(expectedPost.getContent(), dto.getContent()),
                        () -> assertEquals(expectedPost.getReadingTimeMinutes(), dto.getReadingTimeMinutes()),
                        () -> assertEquals(expectedPost.getLikeCount(), dto.getLikeCount()),
                        () -> assertEquals(expectedPost.getCommentCount(), dto.getCommentCount()),
                        () -> assertEquals(expectedPost.getViewCount(), dto.getViewCount()),
                        () -> assertNotNull(dto.getUser()),
                        () -> assertEquals(expectedPost.getUser().getId(), dto.getUser().getId()),
                        () -> assertEquals(expectedPost.getUser().getName(), dto.getUser().getName()),
                        () -> assertEquals(expectedPost.getUser().getUsername(), dto.getUser().getUsername()),
                        () -> assertEquals(expectedPost.getUser().getActive(), dto.getUser().getActive()),
                        () -> assertNotNull(dto.getCategory()),
                        () -> assertEquals(expectedPost.getCategory().getId(), dto.getCategory().getId()),
                        () -> assertEquals(expectedPost.getCategory().getSlug(), dto.getCategory().getSlug()),
                        () -> assertEquals(expectedPost.getCategory().getName(), dto.getCategory().getName()),
                        () -> assertEquals(expectedPost.getCategory().getDescription(), dto.getCategory().getDescription()),
                        () -> assertEquals(expectedPost.getUser().getId().equals(currentUser.getId()), dto.getIsOwner()),
                        () -> assertEquals(expectedPost.getId().equals(ownedAndLiked.getId()) || expectedPost.getId().equals(followedAndLiked.getId()), dto.getIsLiked())
                );
            }
        }

        @Test
        @DisplayName("returns only category posts older than the provided cursor")
        public void returnsOnlyCategoryPostsOlderThanCursor() {
            PostEntity first = savePost(currentUser, categoryOne, "first", PostStatus.PUBLISHED, null);
            PostEntity second = savePost(authorOne, categoryOne, "second", PostStatus.PUBLISHED, null);
            PostEntity third = savePost(authorTwo, categoryOne, "third", PostStatus.PUBLISHED, null);

            List<PostEntity> ordered = new ArrayList<>(List.of(first, second, third));
            ordered.sort(Comparator.comparing(PostEntity::getId).reversed());
            UUID cursor = ordered.get(1).getId();

            Slice<PostResponseDTO> slice = postRepository.findPostsByCategory(
                    categoryOne.getId(),
                    cursor,
                    currentUser.getId(),
                    PageRequest.of(0, 10)
            );

            assertEquals(1, slice.getNumberOfElements());
            assertFalse(slice.hasNext());
            assertTrue(slice.getContent().stream().allMatch(dto -> dto.getId().compareTo(cursor) < 0));
        }

        @Test
        @DisplayName("returns an empty slice when the category has no posts")
        public void returnsEmptySliceWhenCategoryHasNoPosts() {
            Slice<PostResponseDTO> slice = postRepository.findPostsByCategory(
                    categoryThree.getId(),
                    null,
                    currentUser.getId(),
                    PageRequest.of(0, 10)
            );

            assertEquals(0, slice.getNumberOfElements());
            assertFalse(slice.hasNext());
        }
    }

    @Nested
    @DisplayName("updatePostsCategory(CategoryEntity, CategoryEntity)")
    class UpdatePostsCategory {

        @Test
        @DisplayName("moves only posts in the old category to the new category")
        public void movesOnlyPostsInOldCategoryToNewCategory() {
            PostEntity first = savePost(authorOne, categoryOne, "first", PostStatus.PUBLISHED, null);
            PostEntity second = savePost(authorTwo, categoryOne, "second", PostStatus.DRAFT, null);
            PostEntity untouched = savePost(currentUser, categoryThree, "untouched", PostStatus.PUBLISHED, null);

            postRepository.updatePostsCategory(categoryOne, categoryTwo);
            entityManager.flush();
            entityManager.clear();

            PostEntity updatedFirst = findPost(first.getId()).orElseThrow();
            PostEntity updatedSecond = findPost(second.getId()).orElseThrow();
            PostEntity unchanged = findPost(untouched.getId()).orElseThrow();

            assertAll(
                    () -> assertEquals(categoryTwo.getId(), updatedFirst.getCategory().getId()),
                    () -> assertEquals(categoryTwo.getId(), updatedSecond.getCategory().getId()),
                    () -> assertEquals(categoryThree.getId(), unchanged.getCategory().getId())
            );
        }
    }

    @Nested
    @DisplayName("findAllPosts(PostStatus, UUID, Pageable)")
    class FindAllPosts {

        @Test
        @DisplayName("returns only posts with the requested status in descending id order")
        public void returnsOnlyPostsWithRequestedStatusInDescendingOrder() {
            PostEntity first = savePost(authorOne, categoryOne, "first", PostStatus.PUBLISHED, null);
            PostEntity second = savePost(authorTwo, categoryOne, "second", PostStatus.PUBLISHED, null);
            savePost(currentUser, categoryOne, "draft", PostStatus.DRAFT, null);
            savePost(stranger, categoryTwo, "scheduled", PostStatus.SCHEDULED, LocalDateTime.now().plusHours(1));

            List<PostEntity> expected = new ArrayList<>(List.of(first, second));
            expected.sort(Comparator.comparing(PostEntity::getId).reversed());

            Slice<PostInfoDTO> slice = postRepository.findAllPosts(PostStatus.PUBLISHED, null, PageRequest.of(0, 10));

            assertEquals(2, slice.getNumberOfElements());
            assertFalse(slice.hasNext());

            List<PostInfoDTO> content = slice.getContent();
            for (int i = 0; i < content.size(); i++) {
                PostEntity expectedPost = expected.get(i);
                PostInfoDTO dto = content.get(i);

                assertAll(
                        () -> assertEquals(expectedPost.getId(), dto.getId()),
                        () -> assertEquals(expectedPost.getSlug(), dto.getSlug()),
                        () -> assertEquals(expectedPost.getTitle(), dto.getTitle()),
                        () -> assertEquals(expectedPost.getDescription(), dto.getDescription()),
                        () -> assertEquals(expectedPost.getReadingTimeMinutes(), dto.getReadingTimeMinutes()),
                        () -> assertEquals(expectedPost.getLikeCount(), dto.getLikeCount()),
                        () -> assertEquals(expectedPost.getCommentCount(), dto.getCommentCount()),
                        () -> assertEquals(expectedPost.getViewCount(), dto.getViewCount())
                );
            }
        }

        @Test
        @DisplayName("returns an empty slice when no posts match the requested status")
        public void returnsEmptySliceWhenNoPostsMatchRequestedStatus() {
            Slice<PostInfoDTO> slice = postRepository.findAllPosts(PostStatus.SCHEDULED, null, PageRequest.of(0, 10));

            assertEquals(0, slice.getNumberOfElements());
            assertFalse(slice.hasNext());
        }
    }

    @Nested
    @DisplayName("findAllPostsOfFollowings(UUID, PostStatus, UUID, Pageable)")
    class FindAllPostsOfFollowings {

        @Test
        @DisplayName("returns published posts from followed users in descending id order")
        public void returnsPublishedPostsFromFollowedUsersInDescendingOrder() {
            saveFollow(currentUser, authorOne);
            saveFollow(currentUser, authorTwo);

            PostEntity first = savePost(authorOne, categoryOne, "first", PostStatus.PUBLISHED, null);
            PostEntity second = savePost(authorOne, categoryOne, "second", PostStatus.PUBLISHED, null);
            PostEntity third = savePost(authorTwo, categoryTwo, "third", PostStatus.PUBLISHED, null);
            savePost(currentUser, categoryThree, "self", PostStatus.PUBLISHED, null);
            savePost(authorOne, categoryOne, "draft", PostStatus.DRAFT, null);

            List<PostEntity> expected = new ArrayList<>(List.of(first, second, third));
            expected.sort(Comparator.comparing(PostEntity::getId).reversed());

            Slice<PostInfoDTO> slice = postRepository.findAllPostsOfFollowings(
                    currentUser.getId(),
                    PostStatus.PUBLISHED,
                    null,
                    PageRequest.of(0, 2)
            );

            assertEquals(2, slice.getNumberOfElements());
            assertTrue(slice.hasNext());
            assertTrue(slice.getContent().stream().allMatch(dto -> expected.stream().anyMatch(post -> post.getId().equals(dto.getId()))));
        }

        @Test
        @DisplayName("returns an empty slice when the user follows nobody")
        public void returnsEmptySliceWhenUserFollowsNobody() {
            Slice<PostInfoDTO> slice = postRepository.findAllPostsOfFollowings(
                    stranger.getId(),
                    PostStatus.PUBLISHED,
                    null,
                    PageRequest.of(0, 10)
            );

            assertEquals(0, slice.getNumberOfElements());
            assertFalse(slice.hasNext());
        }
    }

    @Nested
    @DisplayName("incrementLikeCount(UUID) and decrementLikeCount(UUID)")
    class LikeCountUpdates {

        @Test
        @DisplayName("updates the like count for the requested post and leaves other posts unchanged")
        public void updatesLikeCountForRequestedPost() {
            PostEntity target = savePost(authorOne, categoryOne, "target", PostStatus.PUBLISHED, null);
            PostEntity other = savePost(authorTwo, categoryOne, "other", PostStatus.PUBLISHED, null);

            assertEquals(1, postRepository.incrementLikeCount(target.getId()));
            assertEquals(1, postRepository.decrementLikeCount(target.getId()));
            assertEquals(0, postRepository.incrementLikeCount(UUID.randomUUID()));

            postRepository.flush();

            PostEntity refreshedTarget = findPost(target.getId()).orElseThrow();
            PostEntity refreshedOther = findPost(other.getId()).orElseThrow();

            assertAll(
                    () -> assertEquals(0, refreshedTarget.getLikeCount()),
                    () -> assertEquals(0, refreshedOther.getLikeCount())
            );
        }
    }

    @Nested
    @DisplayName("incrementCommentCount(UUID) and decrementCommentCount(UUID)")
    class CommentCountUpdates {

        @Test
        @DisplayName("updates the comment count for the requested post and leaves other posts unchanged")
        public void updatesCommentCountForRequestedPost() {
            PostEntity target = savePost(authorOne, categoryOne, "target", PostStatus.PUBLISHED, null);
            PostEntity other = savePost(authorTwo, categoryOne, "other", PostStatus.PUBLISHED, null);

            assertEquals(1, postRepository.incrementCommentCount(target.getId()));
            assertEquals(1, postRepository.decrementCommentCount(target.getId()));
            assertEquals(0, postRepository.incrementCommentCount(UUID.randomUUID()));

            postRepository.flush();

            PostEntity refreshedTarget = findPost(target.getId()).orElseThrow();
            PostEntity refreshedOther = findPost(other.getId()).orElseThrow();

            assertAll(
                    () -> assertEquals(0, refreshedTarget.getCommentCount()),
                    () -> assertEquals(0, refreshedOther.getCommentCount())
            );
        }
    }

    @Nested
    @DisplayName("incrementViewCount(UUID, Long)")
    class ViewCountUpdates {

        @Test
        @DisplayName("updates the view count by the provided delta and ignores unknown post ids")
        public void updatesViewCountByProvidedDelta() {
            PostEntity target = savePost(authorOne, categoryOne, "target", PostStatus.PUBLISHED, null);
            PostEntity other = savePost(authorTwo, categoryOne, "other", PostStatus.PUBLISHED, null);

            postRepository.incrementViewCount(target.getId(), 5L);
            postRepository.incrementViewCount(UUID.randomUUID(), 10L);

            entityManager.flush();
            entityManager.clear();

            PostEntity refreshedTarget = findPost(target.getId()).orElseThrow();
            PostEntity refreshedOther = findPost(other.getId()).orElseThrow();

            assertAll(
                    () -> assertEquals(5L, refreshedTarget.getViewCount()),
                    () -> assertEquals(0L, refreshedOther.getViewCount())
            );
        }
    }

    @Nested
    @DisplayName("publishDuePosts(LocalDateTime)")
    class PublishDuePosts {

        @Test
        @DisplayName("publishes only scheduled posts whose publish time is due")
        public void publishesOnlyDueScheduledPosts() {
            LocalDateTime now = LocalDateTime.now();
            PostEntity due = savePost(authorOne, categoryOne, "due", PostStatus.SCHEDULED, now.minusMinutes(5));
            PostEntity future = savePost(authorOne, categoryOne, "future", PostStatus.SCHEDULED, now.plusHours(1));
            PostEntity alreadyPublished = savePost(authorOne, categoryOne, "published", PostStatus.PUBLISHED, null);

            int updated = postRepository.publishDuePosts(now);
            entityManager.flush();
            entityManager.clear();

            PostEntity refreshedDue = findPost(due.getId()).orElseThrow();
            PostEntity refreshedFuture = findPost(future.getId()).orElseThrow();
            PostEntity refreshedPublished = findPost(alreadyPublished.getId()).orElseThrow();

            assertAll(
                    () -> assertEquals(1, updated),
                    () -> assertEquals(PostStatus.PUBLISHED, refreshedDue.getStatus()),
                    () -> assertNull(refreshedDue.getPublishAt()),
                    () -> assertEquals(PostStatus.SCHEDULED, refreshedFuture.getStatus()),
                    () -> assertNotNull(refreshedFuture.getPublishAt()),
                    () -> assertTrue(refreshedFuture.getPublishAt().isAfter(now)),
                    () -> assertEquals(PostStatus.PUBLISHED, refreshedPublished.getStatus()),
                    () -> assertNull(refreshedPublished.getPublishAt())
            );
        }
    }

    @Nested
    @DisplayName("findByUserIdAndStatus(UUID, PostStatus, UUID, Pageable)")
    class FindByUserIdAndStatus {

        @Test
        @DisplayName("returns only posts for the requested user and status in descending id order")
        public void returnsOnlyPostsForRequestedUserAndStatusInDescendingOrder() {
            PostEntity first = savePost(authorOne, categoryOne, "first", PostStatus.PUBLISHED, null);
            PostEntity second = savePost(authorOne, categoryTwo, "second", PostStatus.PUBLISHED, null);
            savePost(authorOne, categoryTwo, "draft", PostStatus.DRAFT, null);
            savePost(authorTwo, categoryOne, "other", PostStatus.PUBLISHED, null);

            List<PostEntity> expected = new ArrayList<>(List.of(first, second));
            expected.sort(Comparator.comparing(PostEntity::getId).reversed());

            Slice<PostInfoDTO> slice = postRepository.findByUserIdAndStatus(
                    authorOne.getId(),
                    PostStatus.PUBLISHED,
                    null,
                    PageRequest.of(0, 10)
            );

            assertEquals(2, slice.getNumberOfElements());
            assertFalse(slice.hasNext());

            List<PostInfoDTO> content = slice.getContent();
            for (int i = 0; i < content.size(); i++) {
                PostEntity expectedPost = expected.get(i);
                PostInfoDTO dto = content.get(i);

                assertAll(
                        () -> assertEquals(expectedPost.getId(), dto.getId()),
                        () -> assertEquals(expectedPost.getSlug(), dto.getSlug()),
                        () -> assertEquals(expectedPost.getTitle(), dto.getTitle()),
                        () -> assertEquals(expectedPost.getDescription(), dto.getDescription()),
                        () -> assertEquals(expectedPost.getReadingTimeMinutes(), dto.getReadingTimeMinutes()),
                        () -> assertEquals(expectedPost.getLikeCount(), dto.getLikeCount()),
                        () -> assertEquals(expectedPost.getCommentCount(), dto.getCommentCount()),
                        () -> assertEquals(expectedPost.getViewCount(), dto.getViewCount())
                );
            }
        }

        @Test
        @DisplayName("returns only posts older than the provided cursor")
        public void returnsOnlyUserPostsOlderThanCursor() {
            PostEntity first = savePost(authorOne, categoryOne, "first", PostStatus.PUBLISHED, null);
            PostEntity second = savePost(authorOne, categoryOne, "second", PostStatus.PUBLISHED, null);
            PostEntity third = savePost(authorOne, categoryOne, "third", PostStatus.PUBLISHED, null);

            List<PostEntity> ordered = new ArrayList<>(List.of(first, second, third));
            ordered.sort(Comparator.comparing(PostEntity::getId).reversed());
            UUID cursor = ordered.get(1).getId();

            Slice<PostInfoDTO> slice = postRepository.findByUserIdAndStatus(
                    authorOne.getId(),
                    PostStatus.PUBLISHED,
                    cursor,
                    PageRequest.of(0, 10)
            );

            assertEquals(1, slice.getNumberOfElements());
            assertFalse(slice.hasNext());
            assertTrue(slice.getContent().stream().allMatch(dto -> dto.getId().compareTo(cursor) < 0));
        }
    }

    @Nested
    @DisplayName("findByStatusAndPublishAtLessThanEqual(PostStatus, LocalDateTime)")
    class FindByStatusAndPublishAtLessThanEqual {

        @Test
        @DisplayName("returns only scheduled posts whose publish time is due")
        public void returnsOnlyScheduledPostsWhosePublishTimeIsDue() {
            LocalDateTime now = LocalDateTime.now();
            PostEntity due = savePost(authorOne, categoryOne, "due", PostStatus.SCHEDULED, now.minusMinutes(10));
            savePost(authorOne, categoryOne, "future", PostStatus.SCHEDULED, now.plusMinutes(10));
            savePost(authorOne, categoryOne, "draft", PostStatus.DRAFT, now.minusMinutes(10));

            List<PostEntity> found = postRepository.findByStatusAndPublishAtLessThanEqual(PostStatus.SCHEDULED, now);

            assertEquals(1, found.size());
            PostEntity firstFound = found.stream().findFirst().orElseThrow();
            assertEquals(due.getId(), firstFound.getId());
        }
    }

    private @NonNull UserEntity saveUser(String suffix) {
        return userRepository.saveAndFlush(TestEntityFactory.testUser(suffix));
    }

    private @NonNull CategoryEntity saveCategory(String suffix) {
        return categoryRepository.saveAndFlush(TestEntityFactory.testCategory(suffix));
    }

    private @NonNull PostEntity savePost(UserEntity owner, CategoryEntity category, String suffix, PostStatus status, LocalDateTime publishAt) {
        PostEntity post = TestEntityFactory.testPost(owner, category, suffix);
        post.setStatus(status);
        post.setPublishAt(publishAt);
        return postRepository.saveAndFlush(post);
    }

    private void saveLike(UserEntity likeUser, PostEntity targetPost) {
        likeRepository.saveAndFlush(TestEntityFactory.testLike(likeUser, targetPost));
    }

    private void saveFollow(UserEntity follower, UserEntity following) {
        followRepository.saveAndFlush(TestEntityFactory.testFollow(follower, following));
    }

    private @NonNull Optional<PostEntity> findPost(UUID id) {
        return postRepository.findById(id);
    }
}

