package com.project.Blog_Management_System.Repositories;

import com.project.Blog_Management_System.Dto.CommentResponseDTO;
import com.project.Blog_Management_System.Entities.CategoryEntity;
import com.project.Blog_Management_System.Entities.CommentEntity;
import com.project.Blog_Management_System.Entities.PostEntity;
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
import org.springframework.data.domain.Slice;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
public class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private EntityManager entityManager;

    private UserEntity author;
    private UserEntity otherUser;
    private PostEntity post;
    private PostEntity otherPost;

    @BeforeEach
    public void setUp() {
        author = saveUser("author");
        otherUser = saveUser("other-user");
        CategoryEntity category = saveCategory("category");
        CategoryEntity otherCategory = saveCategory("other-category");
        post = savePost(author, category, "post");
        otherPost = savePost(otherUser, otherCategory, "other-post");
    }

    @Nested
    @DisplayName("findTopLevelByPost(UUID, UUID, UUID, Pageable)")
    class FindTopLevelByPost {

        @Test
        @DisplayName("returns top-level comments for the post ordered by id descending with author and reply flags")
        public void returnsTopLevelCommentsInDescendingOrderWithProjection() {
            CommentEntity first = saveTopLevelComment(post, author, "first top-level");
            CommentEntity second = saveTopLevelComment(post, otherUser, "second top-level");
            CommentEntity third = saveTopLevelComment(post, author, "third top-level");
            saveReply(post, first, otherUser, "reply to first");
            saveTopLevelComment(otherPost, otherUser, "other post top-level");

            List<CommentEntity> expected = new ArrayList<>(List.of(first, second, third));
            expected.sort(Comparator.comparing(CommentEntity::getId).reversed());

            Slice<CommentResponseDTO> slice = commentRepository.findTopLevelByPost(
                    post.getId(),
                    null,
                    author.getId(),
                    PageRequest.of(0, 10)
            );

            assertEquals(3, slice.getNumberOfElements());
            assertFalse(slice.hasNext());

            List<CommentResponseDTO> content = slice.getContent();
            for (int i = 0; i < content.size(); i++) {
                CommentEntity expectedComment = expected.get(i);
                CommentResponseDTO dto = content.get(i);

                assertAll(
                        () -> assertEquals(expectedComment.getId(), dto.getId()),
                        () -> assertEquals(expectedComment.getBody(), dto.getBody()),
                        () -> assertNotNull(dto.getUser()),
                        () -> assertEquals(expectedComment.getUser().getId(), dto.getUser().getId()),
                        () -> assertEquals(expectedComment.getUser().getName(), dto.getUser().getName()),
                        () -> assertEquals(expectedComment.getUser().getUsername(), dto.getUser().getUsername()),
                        () -> assertEquals(expectedComment.getUser().getActive(), dto.getUser().getActive()),
                        () -> assertNull(dto.getParentId()),
                        () -> assertEquals(expectedComment.getId().equals(first.getId()), dto.getHasReplies()),
                        () -> assertEquals(expectedComment.getUser().getId().equals(author.getId()), dto.getIsAuthor()),
                        () -> assertNotNull(dto.getCreatedAt())
                );
            }
        }

        @Test
        @DisplayName("returns only comments for the requested post and excludes replies")
        public void returnsOnlyCommentsForRequestedPost() {
            saveTopLevelComment(post, author, "top-level on requested post");
            saveReply(post, saveTopLevelComment(post, author, "parent with reply"), otherUser, "nested reply");
            saveTopLevelComment(otherPost, otherUser, "top-level on other post");

            Slice<CommentResponseDTO> slice = commentRepository.findTopLevelByPost(
                    post.getId(),
                    null,
                    author.getId(),
                    PageRequest.of(0, 10)
            );

            assertEquals(2, slice.getNumberOfElements());
            assertTrue(slice.getContent().stream().allMatch(dto -> dto.getParentId() == null));
            assertTrue(slice.getContent().stream().allMatch(CommentResponseDTO::getIsAuthor));
        }

        @Test
        @DisplayName("returns only comments older than the provided cursor")
        public void returnsOnlyCommentsOlderThanCursor() {
            CommentEntity first = saveTopLevelComment(post, author, "first top-level");
            CommentEntity second = saveTopLevelComment(post, otherUser, "second top-level");
            CommentEntity third = saveTopLevelComment(post, author, "third top-level");

            List<CommentEntity> ordered = new ArrayList<>(List.of(first, second, third));
            ordered.sort(Comparator.comparing(CommentEntity::getId).reversed());
            UUID cursor = ordered.get(1).getId();

            Slice<CommentResponseDTO> slice = commentRepository.findTopLevelByPost(
                    post.getId(),
                    cursor,
                    author.getId(),
                    PageRequest.of(0, 10)
            );

            assertEquals(1, slice.getNumberOfElements());
            assertFalse(slice.hasNext());
            assertTrue(slice.getContent().stream().allMatch(dto -> dto.getId().compareTo(cursor) < 0));
        }

        @Test
        @DisplayName("returns an empty slice when the post has no top-level comments")
        public void returnsEmptySliceWhenNoTopLevelCommentsExist() {
            saveTopLevelComment(otherPost, otherUser, "other post top-level");

            Slice<CommentResponseDTO> slice = commentRepository.findTopLevelByPost(
                    post.getId(),
                    null,
                    author.getId(),
                    PageRequest.of(0, 10)
            );

            assertEquals(0, slice.getNumberOfElements());
            assertFalse(slice.hasNext());
        }

        @Test
        @DisplayName("returns hasNext when the page size is smaller than the number of top-level comments")
        public void returnsHasNextWhenPageSizeIsSmallerThanResultSize() {
            saveTopLevelComment(post, author, "top-level-1");
            saveTopLevelComment(post, otherUser, "top-level-2");
            saveTopLevelComment(post, author, "top-level-3");

            Slice<CommentResponseDTO> slice = commentRepository.findTopLevelByPost(
                    post.getId(),
                    null,
                    author.getId(),
                    PageRequest.of(0, 2)
            );

            assertEquals(2, slice.getNumberOfElements());
            assertTrue(slice.hasNext());
        }
    }

    @Nested
    @DisplayName("findRepliesByParentId(UUID, UUID, UUID, Pageable)")
    class FindRepliesByParentId {

        @Test
        @DisplayName("returns replies for the parent comment ordered by id descending with author and reply flags")
        public void returnsRepliesInDescendingOrderWithProjection() {
            CommentEntity parent = saveTopLevelComment(post, author, "parent comment");
            CommentEntity firstReply = saveReply(post, parent, author, "first reply");
            CommentEntity secondReply = saveReply(post, parent, otherUser, "second reply");
            CommentEntity thirdReply = saveReply(post, parent, author, "third reply");
            saveTopLevelComment(post, otherUser, "top-level comment");

            List<CommentEntity> expected = new ArrayList<>(List.of(firstReply, secondReply, thirdReply));
            expected.sort(Comparator.comparing(CommentEntity::getId).reversed());

            Slice<CommentResponseDTO> slice = commentRepository.findRepliesByParentId(
                    parent.getId(),
                    null,
                    author.getId(),
                    PageRequest.of(0, 10)
            );

            assertEquals(3, slice.getNumberOfElements());
            assertFalse(slice.hasNext());

            List<CommentResponseDTO> content = slice.getContent();
            for (int i = 0; i < content.size(); i++) {
                CommentEntity expectedComment = expected.get(i);
                CommentResponseDTO dto = content.get(i);

                assertAll(
                        () -> assertEquals(expectedComment.getId(), dto.getId()),
                        () -> assertEquals(expectedComment.getBody(), dto.getBody()),
                        () -> assertNotNull(dto.getUser()),
                        () -> assertEquals(expectedComment.getUser().getId(), dto.getUser().getId()),
                        () -> assertEquals(expectedComment.getUser().getName(), dto.getUser().getName()),
                        () -> assertEquals(expectedComment.getUser().getUsername(), dto.getUser().getUsername()),
                        () -> assertEquals(expectedComment.getUser().getActive(), dto.getUser().getActive()),
                        () -> assertEquals(parent.getId(), dto.getParentId()),
                        () -> assertEquals(expectedComment.getUser().getId().equals(author.getId()), dto.getIsAuthor()),
                        () -> assertNotNull(dto.getCreatedAt())
                );
            }
        }

        @Test
        @DisplayName("returns only direct replies of the requested parent and excludes top-level comments")
        public void returnsOnlyDirectRepliesOfRequestedParent() {
            CommentEntity parent = saveTopLevelComment(post, author, "parent comment");
            saveReply(post, parent, author, "reply to parent");
            saveReply(post, saveTopLevelComment(post, otherUser, "other parent"), otherUser, "reply to other parent");
            saveTopLevelComment(post, otherUser, "top-level comment");

            Slice<CommentResponseDTO> slice = commentRepository.findRepliesByParentId(
                    parent.getId(),
                    null,
                    author.getId(),
                    PageRequest.of(0, 10)
            );

            assertEquals(1, slice.getNumberOfElements());
            assertTrue(slice.getContent().stream().allMatch(dto -> parent.getId().equals(dto.getParentId())));
        }

        @Test
        @DisplayName("returns only replies older than the provided cursor")
        public void returnsOnlyRepliesOlderThanCursor() {
            CommentEntity parent = saveTopLevelComment(post, author, "parent comment");
            CommentEntity firstReply = saveReply(post, parent, author, "first reply");
            CommentEntity secondReply = saveReply(post, parent, otherUser, "second reply");
            CommentEntity thirdReply = saveReply(post, parent, author, "third reply");

            List<CommentEntity> ordered = new ArrayList<>(List.of(firstReply, secondReply, thirdReply));
            ordered.sort(Comparator.comparing(CommentEntity::getId).reversed());
            UUID cursor = ordered.get(1).getId();

            Slice<CommentResponseDTO> slice = commentRepository.findRepliesByParentId(
                    parent.getId(),
                    cursor,
                    author.getId(),
                    PageRequest.of(0, 10)
            );

            assertEquals(1, slice.getNumberOfElements());
            assertFalse(slice.hasNext());
            assertTrue(slice.getContent().stream().allMatch(dto -> dto.getId().compareTo(cursor) < 0));
        }

        @Test
        @DisplayName("returns an empty slice when the parent has no replies")
        public void returnsEmptySliceWhenParentHasNoReplies() {
            CommentEntity parent = saveTopLevelComment(post, author, "parent comment");

            Slice<CommentResponseDTO> slice = commentRepository.findRepliesByParentId(
                    parent.getId(),
                    null,
                    author.getId(),
                    PageRequest.of(0, 10)
            );

            assertEquals(0, slice.getNumberOfElements());
            assertFalse(slice.hasNext());
        }

        @Test
        @DisplayName("returns hasNext when the page size is smaller than the number of replies")
        public void returnsHasNextWhenPageSizeIsSmallerThanResultSize() {
            CommentEntity parent = saveTopLevelComment(post, author, "parent comment");
            saveReply(post, parent, author, "reply-1");
            saveReply(post, parent, otherUser, "reply-2");
            saveReply(post, parent, author, "reply-3");

            Slice<CommentResponseDTO> slice = commentRepository.findRepliesByParentId(
                    parent.getId(),
                    null,
                    author.getId(),
                    PageRequest.of(0, 2)
            );

            assertEquals(2, slice.getNumberOfElements());
            assertTrue(slice.hasNext());
        }
    }

    @Nested
    @DisplayName("saveAndFlush()")
    class SaveComment {

        @Test
        @DisplayName("saves a top-level comment successfully with generated id and timestamps")
        public void savesSaveTopLevelCommentSuccessfully() {

            CommentEntity saved = saveTopLevelComment(post, author, "new top-level");

            assertNotNull(saved.getId());
            assertEquals(author.getId(), saved.getUser().getId());
            assertEquals(post.getId(), saved.getPost().getId());
            assertEquals(0, saved.getDepth());
            assertNull(saved.getParent());
            assertNotNull(saved.getCreatedAt());
            assertNotNull(saved.getUpdatedAt());
        }

        @Test
        @DisplayName("saves a reply comment successfully with parent and incremented depth")
        public void savesSaveReplyCommentSuccessfully() {
            CommentEntity parent = saveTopLevelComment(post, author, "parent");

            CommentEntity saved = saveReply(post, parent, otherUser, "new reply");

            assertNotNull(saved.getId());
            assertEquals(otherUser.getId(), saved.getUser().getId());
            assertEquals(post.getId(), saved.getPost().getId());
            assertEquals(1, saved.getDepth());
            assertEquals(parent.getId(), saved.getParent().getId());
            assertNotNull(saved.getCreatedAt());
            assertNotNull(saved.getUpdatedAt());
        }

        @Test
        @DisplayName("updates an existing comment")
        public void updatesExistingComment() {
            CommentEntity comment = saveTopLevelComment(post, author, "original");
            UUID originalId = comment.getId();

            comment.setBody("Updated comment body");
            CommentEntity updated = commentRepository.saveAndFlush(comment);

            assertEquals(originalId, updated.getId());
            assertEquals("Updated comment body", updated.getBody());
        }
    }

    @Nested
    @DisplayName("findById(UUID)")
    class FindById {

        @Test
        @DisplayName("returns the comment when the id exists")
        public void returnsCommentWhenIdExists() {
            CommentEntity saved = saveTopLevelComment(post, author, "test");

            Optional<CommentEntity> found = commentRepository.findById(saved.getId());

            assertTrue(found.isPresent());
            assertEquals(saved.getId(), found.get().getId());
            assertEquals(saved.getBody(), found.get().getBody());
            assertEquals(saved.getDepth(), found.get().getDepth());
        }

        @Test
        @DisplayName("returns empty when no comment exists with the given id")
        public void returnsEmptyWhenIdDoesNotExist() {
            Optional<CommentEntity> found = commentRepository.findById(UUID.randomUUID());

            assertFalse(found.isPresent());
        }

        @Test
        @DisplayName("returns the comment with all relationships loaded")
        public void returnsCommentWithRelationshipsLoaded() {
            CommentEntity parent = saveTopLevelComment(post, author, "parent");
            CommentEntity reply = saveReply(post, parent, otherUser, "reply");

            Optional<CommentEntity> found = commentRepository.findById(reply.getId());

            assertTrue(found.isPresent());
            assertNotNull(found.get().getUser());
            assertEquals(otherUser.getId(), found.get().getUser().getId());
            assertNotNull(found.get().getPost());
            assertEquals(post.getId(), found.get().getPost().getId());
            assertNotNull(found.get().getParent());
            assertEquals(parent.getId(), found.get().getParent().getId());
        }
    }

    @Nested
    @DisplayName("deleteById(UUID)")
    class DeleteComment {

        @Test
        @DisplayName("removes the comment so it can no longer be found")
        public void removesCommentSuccessfully() {
            CommentEntity saved = saveTopLevelComment(post, author, "to-delete");

            commentRepository.deleteById(saved.getId());
            entityManager.flush();
            entityManager.clear();

            Optional<CommentEntity> found = commentRepository.findById(saved.getId());
            assertFalse(found.isPresent());
        }

        @Test
        @DisplayName("does not throw when deleting a non-existent comment")
        public void doesNotThrowWhenDeletingNonExistent() {
            assertDoesNotThrow(() -> {
                commentRepository.deleteById(UUID.randomUUID());
                entityManager.flush();
                entityManager.clear();
            });
        }

        @Test
        @DisplayName("deletes only the specified comment and leaves others intact")
        public void deletesOnlySpecifiedCommentAndLeavesOthersIntact() {
            CommentEntity first = saveTopLevelComment(post, author, "first");
            CommentEntity second = saveTopLevelComment(post, author, "second");

            commentRepository.deleteById(first.getId());
            entityManager.flush();
            entityManager.clear();

            Optional<CommentEntity> deletedFound = commentRepository.findById(first.getId());
            Optional<CommentEntity> retainedFound = commentRepository.findById(second.getId());

            assertFalse(deletedFound.isPresent());
            assertTrue(retainedFound.isPresent());
        }

        @Test
        @DisplayName("deletes the parent comment and cascades deletion of replies")
        public void deleteParentCascadesToReplies() {
            CommentEntity parent = saveTopLevelComment(post, author, "parent");
            CommentEntity firstReply = saveReply(post, parent, otherUser, "first-reply");
            CommentEntity secondReply = saveReply(post, parent, author, "second-reply");

            entityManager.flush();
            entityManager.clear();

            commentRepository.deleteById(parent.getId());

            entityManager.flush();
            entityManager.clear();

            Optional<CommentEntity> parentFound = commentRepository.findById(parent.getId());
            Optional<CommentEntity> firstReplyFound = commentRepository.findById(firstReply.getId());
            var secondReplyFound = commentRepository.findById(secondReply.getId());

            assertFalse(parentFound.isPresent());
            assertFalse(firstReplyFound.isPresent());
            assertFalse(secondReplyFound.isPresent());
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

    private @NonNull CommentEntity saveTopLevelComment(PostEntity targetPost, UserEntity commentAuthor, String suffix) {
        return commentRepository.saveAndFlush(TestEntityFactory.testTopLevelComment(targetPost, commentAuthor, suffix));
    }

    private @NonNull CommentEntity saveReply(PostEntity targetPost, CommentEntity parent, UserEntity commentAuthor, String suffix) {
        return commentRepository.saveAndFlush(TestEntityFactory.testReplyComment(targetPost, parent, commentAuthor, suffix));
    }
}
