package com.project.Blog_Management_System.Repositories;

import com.project.Blog_Management_System.Dto.BookmarkInfoDTO;
import com.project.Blog_Management_System.Entities.BookmarkEntity;
import com.project.Blog_Management_System.Entities.CategoryEntity;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;


@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
public class BookmarkRepositoryTest {

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    public UserRepository userRepository;

    @Autowired
    public PostRepository postRepository;

    @Autowired
    public CategoryRepository categoryRepository;

    private UserEntity user;
    private PostEntity post;

    @BeforeEach
    public void setUp() {
        user = userRepository.saveAndFlush(TestEntityFactory.testUser("1"));
        CategoryEntity category = categoryRepository.saveAndFlush(TestEntityFactory.testCategory("1"));
        post = postRepository.saveAndFlush(TestEntityFactory.testPost(user, category, "1"));
    }

    @Nested
    @DisplayName("findByUserIdAndPostId(UUID, UUID)")
    class FindByUserIdAndPostId {

        @Test
        @DisplayName("returns the bookmark when the user and post match an existing bookmark")
        public void returnsBookmarkWhenPresent() {
            BookmarkEntity bookmark = saveBookmark(user, post);

            Optional<BookmarkEntity> found = bookmarkRepository.findByUserIdAndPostId(user.getId(), post.getId());

            assertTrue(found.isPresent());
            assertEquals(bookmark.getId(), found.get().getId());
            assertEquals(user, found.get().getUser());
            assertEquals(post, found.get().getPost());
        }

        @Test
        @DisplayName("returns empty when the user does not match any bookmark")
        public void returnsEmptyWhenUserDoesNotMatch() {
            saveBookmark(user, post);
            UserEntity otherUser = saveUser("other-user");

            Optional<BookmarkEntity> found = bookmarkRepository.findByUserIdAndPostId(otherUser.getId(), post.getId());

            assertFalse(found.isPresent());
        }

        @Test
        @DisplayName("returns empty when the post does not match any bookmark")
        public void returnsEmptyWhenPostDoesNotMatch() {
            saveBookmark(user, post);
            CategoryEntity otherCategory = saveCategory("other-category");
            PostEntity otherPost = savePost(user, otherCategory, "other-bookmark-post");

            Optional<BookmarkEntity> found = bookmarkRepository.findByUserIdAndPostId(user.getId(), otherPost.getId());

            assertFalse(found.isPresent());
        }

        @Test
        @DisplayName("returns empty when neither the user nor the post match any bookmark")
        public void returnsEmptyWhenNeitherIdMatches() {
            saveBookmark(user, post);

            Optional<BookmarkEntity> found = bookmarkRepository.findByUserIdAndPostId(UUID.randomUUID(), UUID.randomUUID());

            assertFalse(found.isPresent());
        }
    }

    @Nested
    @DisplayName("deleteByUserIdAndPostId(UUID, UUID)")
    class DeleteByUserIdAndPostId {

        @Test
        @DisplayName("removes the matching bookmark")
        public void removesMatchingBookmark() {
            saveBookmark(user, post);

            bookmarkRepository.deleteByUserIdAndPostId(user.getId(), post.getId());

            Optional<BookmarkEntity> found = bookmarkRepository.findByUserIdAndPostId(user.getId(), post.getId());

            assertFalse(found.isPresent());
        }

        @Test
        @DisplayName("does not throw when the bookmark does not exist")
        public void doesNotThrowWhenBookmarkDoesNotExist() {
            assertDoesNotThrow(() -> {
                bookmarkRepository.deleteByUserIdAndPostId(UUID.randomUUID(), UUID.randomUUID());
                bookmarkRepository.flush();
            });
        }

        @Test
        @DisplayName("deletes only the matching bookmark and leaves other bookmarks intact")
        public void deletesOnlyMatchingBookmark() {
            saveBookmark(user, post);

            UserEntity otherUser = saveUser("other-user");
            CategoryEntity otherCategory = saveCategory("other-category");
            PostEntity otherPost = savePost(otherUser, otherCategory, "other-bookmark-post");
            BookmarkEntity otherBookmark = saveBookmark(otherUser, otherPost);

            bookmarkRepository.deleteByUserIdAndPostId(user.getId(), post.getId());

            Optional<BookmarkEntity> deleted = bookmarkRepository.findByUserIdAndPostId(user.getId(), post.getId());
            Optional<BookmarkEntity> retained = bookmarkRepository.findByUserIdAndPostId(otherUser.getId(), otherPost.getId());

            assertFalse(deleted.isPresent());
            assertTrue(retained.isPresent());
            assertEquals(otherBookmark.getId(), retained.get().getId());
        }
    }

    @Nested
    @DisplayName("findByUser(UUID, UUID, Pageable)")
    class FindByUser {

        @Test
        @DisplayName("returns projected bookmarks for the requested user in descending id order")
        public void returnsProjectedBookmarksInDescendingOrder() {
            CategoryEntity category = saveCategory("projection");
            List<BookmarkEntity> bookmarks = new ArrayList<>();
            Map<UUID, PostEntity> postByBookmarkId = new HashMap<>();

            for (int i = 0; i < 3; i++) {
                PostEntity createdPost = savePost(user, category, "projection-post-" + i);
                BookmarkEntity createdBookmark = saveBookmark(user, createdPost);
                bookmarks.add(createdBookmark);
                postByBookmarkId.put(createdBookmark.getId(), createdPost);
            }

            bookmarks.sort(Comparator.comparing(BookmarkEntity::getId).reversed());

            Slice<BookmarkInfoDTO> slice = bookmarkRepository.findByUser(user.getId(), null, PageRequest.of(0, 10));

            assertEquals(bookmarks.size(), slice.getNumberOfElements());
            assertFalse(slice.hasNext());

            List<BookmarkInfoDTO> content = slice.getContent();
            for (int i = 0; i < content.size(); i++) {
                BookmarkEntity expectedBookmark = bookmarks.get(i);
                PostEntity expectedPost = postByBookmarkId.get(expectedBookmark.getId());
                BookmarkInfoDTO dto = content.get(i);

                assertAll(
                        () -> assertEquals(expectedBookmark.getId(), dto.getId()),
                        () -> assertEquals(expectedBookmark.getBookmarkedAt(), dto.getBookmarkedAt()),
                        () -> assertNotNull(dto.getPost()),
                        () -> assertEquals(expectedPost.getId(), dto.getPost().getId()),
                        () -> assertEquals(expectedPost.getSlug(), dto.getPost().getSlug()),
                        () -> assertEquals(expectedPost.getTitle(), dto.getPost().getTitle()),
                        () -> assertEquals(expectedPost.getDescription(), dto.getPost().getDescription()),
                        () -> assertEquals(expectedPost.getReadingTimeMinutes(), dto.getPost().getReadingTimeMinutes()),
                        () -> assertEquals(expectedPost.getLikeCount(), dto.getPost().getLikeCount()),
                        () -> assertEquals(expectedPost.getCommentCount(), dto.getPost().getCommentCount()),
                        () -> assertEquals(expectedPost.getViewCount(), dto.getPost().getViewCount())
                );
            }
        }

        @Test
        @DisplayName("returns hasNext when the page size is smaller than the number of bookmarks")
        public void returnsHasNextWhenPageSizeIsSmallerThanResultSize() {
            CategoryEntity category = saveCategory("page-size");

            for (int i = 0; i < 4; i++) {
                PostEntity createdPost = savePost(user, category, "paged-post-" + i);
                saveBookmark(user, createdPost);
            }

            Slice<BookmarkInfoDTO> slice = bookmarkRepository.findByUser(user.getId(), null, PageRequest.of(0, 2));

            assertEquals(2, slice.getNumberOfElements());
            assertTrue(slice.hasNext());
        }

        @Test
        @DisplayName("returns only bookmarks older than the provided cursor")
        public void returnsOnlyBookmarksOlderThanCursor() {
            CategoryEntity category = saveCategory("cursor");
            List<BookmarkEntity> bookmarks = new ArrayList<>();

            for (int i = 0; i < 4; i++) {
                PostEntity createdPost = savePost(user, category, "cursor-post-" + i);
                bookmarks.add(saveBookmark(user, createdPost));
            }

            bookmarks.sort(Comparator.comparing(BookmarkEntity::getId).reversed());
            UUID cursor = bookmarks.get(1).getId();

            Slice<BookmarkInfoDTO> slice = bookmarkRepository.findByUser(user.getId(), cursor, PageRequest.of(0, 10));

            assertEquals(2, slice.getNumberOfElements());
            assertFalse(slice.hasNext());
            assertTrue(slice.getContent().stream().allMatch(dto -> dto.getId().compareTo(cursor) < 0));
        }

        @Test
        @DisplayName("returns an empty slice when the user has no bookmarks")
        public void returnsEmptySliceWhenUserHasNoBookmarks() {
            UserEntity otherUser = saveUser("empty-user");

            Slice<BookmarkInfoDTO> slice = bookmarkRepository.findByUser(otherUser.getId(), null, PageRequest.of(0, 10));

            assertEquals(0, slice.getNumberOfElements());
            assertFalse(slice.hasNext());
        }
    }

    private @NonNull BookmarkEntity saveBookmark(UserEntity bookmarkUser, PostEntity bookmarkPost) {
        return bookmarkRepository.saveAndFlush(TestEntityFactory.testBookmark(bookmarkUser, bookmarkPost));
    }

    private @NonNull UserEntity saveUser(String suffix) {
        return userRepository.saveAndFlush(TestEntityFactory.testUser(suffix));
    }

    private @NonNull CategoryEntity saveCategory(String suffix) {
        return categoryRepository.saveAndFlush(TestEntityFactory.testCategory(suffix));
    }

    private @NonNull PostEntity savePost(UserEntity owner, CategoryEntity category, String title) {
        return postRepository.saveAndFlush(TestEntityFactory.testPost(owner, category, title));
    }
}
