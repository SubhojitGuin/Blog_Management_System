package com.project.Blog_Management_System.Service;

import com.project.Blog_Management_System.Dto.*;
import com.project.Blog_Management_System.Entities.*;
import com.project.Blog_Management_System.Enums.PostStatus;
import com.project.Blog_Management_System.Enums.Role;
import com.project.Blog_Management_System.Exceptions.ResourceConflictException;
import com.project.Blog_Management_System.Exceptions.ResourceNotFoundException;
import com.project.Blog_Management_System.Repositories.*;
import com.project.Blog_Management_System.Service.Interfaces.PostService;
import com.project.Blog_Management_System.Service.Interfaces.RedisViewCountService;
import com.project.Blog_Management_System.Specifications.PostFilterSpecification;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.project.Blog_Management_System.Utils.AppUtils.*;
import static com.project.Blog_Management_System.Utils.ValidationUtils.*;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final ModelMapper modelMapper;
    private final CategoryRepository categoryRepository;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final BookmarkRepository bookmarkRepository;
    private final RedisViewCountService redisViewCountService;

    @Override
    @Transactional
    public PostResponseDTO createPost(PostRequestDTO postRequestDTO) {
        UserEntity user = getCurrentUser();

        String slug = postRequestDTO.getCategorySlug();
        CategoryEntity category = categoryRepository.findBySlug(slug).orElse(null);
        isInvalidCategory(category, slug);

        PostEntity post = modelMapper.map(postRequestDTO, PostEntity.class);
        post.setUser(user);
        post.setSlug(generateSlug(postRequestDTO.getTitle()));
        post.setCategory(category);
        applyStatusAndPublishAt(post, postRequestDTO.getStatus(), postRequestDTO.getPublishAt());
        PostEntity savedPost = postRepository.saveAndFlush(post);

        int rowsUpdated = userRepository.incrementPostCount(user.getId());
        if (rowsUpdated == 0)
            throw new ResourceConflictException("Failed to increment posts count of the user. Possible concurrent modification.");

        return modelMapper.map(savedPost, PostResponseDTO.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<PostInfoDTO> getAllPosts(UUID postCursor, int size) {
        return postRepository.findAllPosts(PostStatus.PUBLISHED, postCursor, PageRequest.of(0, size));
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<PostInfoDTO> getAllPostsOfFollowings(UUID postCursor, int size) {
        UserEntity user = getCurrentUser();
        return postRepository.findAllPostsOfFollowings(user.getId(), PostStatus.PUBLISHED, postCursor, PageRequest.of(0, size));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostInfoDTO> searchPosts(PostFilterRequestDTO postFilterRequestDTO, int page, int size, List<String> sort) {

        final Set<String> ALLOWED_SORT_FIELDS = Set.of(
                PostEntity.Fields.title,
                PostEntity.Fields.createdAt,
                PostEntity.Fields.updatedAt,
                PostEntity.Fields.likeCount,
                PostEntity.Fields.commentCount,
                PostEntity.Fields.readingTimeMinutes
        );

        Specification<PostEntity> spec = PostFilterSpecification.buildSpecification(postFilterRequestDTO);
        return postRepository.findAll(spec, PageRequest.of(page, size, convertToSort(sort, ALLOWED_SORT_FIELDS)))
                .map(post -> modelMapper.map(post, PostInfoDTO.class));
    }

    @Override
    public Slice<PostInfoDTO> getPostsByStatus(PostStatus status, UUID postCursor, int size) {
        UserEntity user = getCurrentUser();
        return postRepository.findByUserIdAndStatus(user.getId(), status, postCursor, PageRequest.of(0, size));
    }

    @Override
    @Transactional
    public PostResponseDTO getPost(String postSlug, UUID postId) {
        UserEntity user = getCurrentUser();

        PostEntity post = postRepository.findById(postId).orElse(null);
        isInvalidPost(post, postSlug);

        if (post.getStatus() != PostStatus.PUBLISHED && !post.getUser().equals(user)) {
            throw new ResourceNotFoundException("Post not found");
        }

        PostResponseDTO postResponseDTO = modelMapper.map(post, PostResponseDTO.class);
        postResponseDTO.setIsOwner(user.equals(post.getUser()));
        postResponseDTO.setIsLiked(likeRepository.findByUserIdAndPostId(user.getId(), post.getId()).isPresent());

        redisViewCountService.addViewer(postId, user.getId());

        return postResponseDTO;
    }

    @Override
    @Transactional
    public PostResponseDTO updatePost(String postSlug, UUID postId, PostRequestDTO postRequestDTO) {
        UserEntity user = getCurrentUser();

        PostEntity post = postRepository.findById(postId).orElse(null);
        isInvalidPost(post, postSlug);

        if (post.getStatus() != PostStatus.PUBLISHED && !post.getUser().equals(user)) {
            throw new ResourceNotFoundException("Post not found");
        }

        if (!post.getUser().equals(user)) {
            throw new AccessDeniedException("You are not authorized to update this post");
        }

        String categorySlug = postRequestDTO.getCategorySlug();
        CategoryEntity category = categoryRepository.findBySlug(categorySlug).orElse(null);
        isInvalidCategory(category, categorySlug);

        post.setTitle(postRequestDTO.getTitle());
        post.setDescription(postRequestDTO.getDescription());
        post.setContent(postRequestDTO.getContent());
        post.setSlug(generateSlug(postRequestDTO.getTitle()));
        post.setCategory(category);
        applyStatusAndPublishAt(post, postRequestDTO.getStatus(), postRequestDTO.getPublishAt());

        postRepository.saveAndFlush(post);

        return modelMapper.map(post, PostResponseDTO.class);
    }

    @Override
    @Transactional
    public void deletePost(String postSlug, UUID postId) {
        UserEntity user = getCurrentUser();

        PostEntity post = postRepository.findById(postId).orElse(null);
        isInvalidPost(post, postSlug);

        if (post.getStatus() != PostStatus.PUBLISHED && !post.getUser().equals(user)) {
            throw new ResourceNotFoundException("Post not found");
        }

        if (!post.getUser().equals(user) && !(hasRole(Role.ADMIN))) {
            throw new AccessDeniedException("You are not authorized to delete this post");
        }

        postRepository.delete(post);

        int rowsUpdated = userRepository.decrementPostCount(post.getUser().getId());
        if (rowsUpdated == 0)
            throw new ResourceConflictException("Failed to decrement posts count of the user. Possible concurrent modification.");
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<CommentResponseDTO> getTopLevelCommentsOfPost(String postSlug, UUID postId, UUID commentCursor, int size) {
        UserEntity user = getCurrentUser();
        PostEntity post = postRepository.findById(postId).orElse(null);
        isInvalidPost(post, postSlug);
        if (post.getStatus() != PostStatus.PUBLISHED && !post.getUser().equals(user)) {
            throw new ResourceNotFoundException("Post not found");
        }

        return commentRepository.findTopLevelByPost(postId, commentCursor, user.getId(), PageRequest.of(0, size));
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<CommentResponseDTO> getRepliesOfComment(String postSlug, UUID postId, UUID parentCommentId, UUID commentCursor, int size) {
        UserEntity user = getCurrentUser();
        PostEntity post = postRepository.findById(postId).orElse(null);
        isInvalidPost(post, postSlug);

        if (post.getStatus() != PostStatus.PUBLISHED && !post.getUser().equals(user)) {
            throw new ResourceNotFoundException("Post not found");
        }

        CommentEntity parent = commentRepository.findById(parentCommentId).orElse(null);
        isInvalidComment(parent);

        if (!parent.getPost().getId().equals(post.getId())) {
            throw new IllegalArgumentException("Parent comment does not belong to this post");
        }

        return commentRepository.findRepliesByParentId(parentCommentId, commentCursor, user.getId(), PageRequest.of(0, size));
    }

    @Override
    @Transactional
    public CommentResponseDTO addTopLevelComments(String postSlug, UUID postId, CommentRequestDTO commentRequestDTO) {
        UserEntity user = getCurrentUser();

        PostEntity post = postRepository.findById(postId).orElse(null);
        isInvalidPost(post, postSlug);
        isPublishedPost(post, user);

        CommentEntity comment = modelMapper.map(commentRequestDTO, CommentEntity.class);
        comment.setUser(user);
        comment.setPost(post);
        CommentEntity savedComment = commentRepository.saveAndFlush(comment);

        int rowsUpdated = postRepository.incrementCommentCount(postId);
        if (rowsUpdated == 0)
            throw new ResourceConflictException("Failed to increment comment count of the post. Possible concurrent modification or stale entity.");

        CommentResponseDTO commentResponseDTO = modelMapper.map(savedComment, CommentResponseDTO.class);
        commentResponseDTO.setIsAuthor(true);
        return commentResponseDTO;
    }

    @Override
    @Transactional
    public CommentResponseDTO addReplyToComment(String postSlug, UUID postId, UUID parentCommentId, CommentRequestDTO commentRequestDTO) {
        UserEntity user = getCurrentUser();

        PostEntity post = postRepository.findById(postId).orElse(null);
        isInvalidPost(post, postSlug);
        isPublishedPost(post, user);

        CommentEntity parent = commentRepository.findById(parentCommentId).orElse(null);
        isInvalidComment(parent);
        validateReplyDepth(parent);

        if (!parent.getPost().getId().equals(post.getId())) {
            throw new IllegalArgumentException("Parent comment does not belong to this post");
        }

        CommentEntity comment = modelMapper.map(commentRequestDTO, CommentEntity.class);
        comment.setUser(user);
        comment.setPost(post);
        comment.setParent(parent);
        comment.setDepth(parent.getDepth() + 1);

        CommentEntity savedComment = commentRepository.saveAndFlush(comment);

        int rowsUpdated = postRepository.incrementCommentCount(postId);
        if (rowsUpdated == 0)
            throw new ResourceConflictException("Failed to increment comment count of the post. Possible concurrent modification or stale entity.");

        CommentResponseDTO commentResponseDTO = modelMapper.map(savedComment, CommentResponseDTO.class);
        commentResponseDTO.setIsAuthor(true);
        return commentResponseDTO;
    }

    @Override
    @Transactional
    public CommentResponseDTO updateComment(String postSlug, UUID postId, UUID commentId, CommentRequestDTO commentRequestDTO) {
        UserEntity user = getCurrentUser();

        PostEntity post = postRepository.findById(postId).orElse(null);
        isInvalidPost(post, postSlug);
        isPublishedPost(post, user);

        CommentEntity comment = commentRepository.findById(commentId).orElse(null);
        isInvalidComment(comment);

        if (!comment.getUser().equals(user)) {
            throw new AccessDeniedException("You are not authorized to update this comment");
        }

        modelMapper.map(commentRequestDTO, comment);

        CommentEntity savedComment = commentRepository.saveAndFlush(comment);
        CommentResponseDTO commentResponseDTO = modelMapper.map(savedComment, CommentResponseDTO.class);
        commentResponseDTO.setIsAuthor(true);
        return commentResponseDTO;
    }

    @Override
    @Transactional
    public void deleteComment(String postSlug, UUID postId, UUID commentId) {
        UserEntity user = getCurrentUser();

        PostEntity post = postRepository.findById(postId).orElse(null);
        isInvalidPost(post, postSlug);
        isPublishedPost(post, user);

        CommentEntity comment = commentRepository.findById(commentId).orElse(null);
        isInvalidComment(comment);

        if (!comment.getUser().equals(user) && !hasRole(Role.ADMIN)) {
            throw new AccessDeniedException("You are not authorized to delete this comment");
        }

        commentRepository.delete(comment);

        int rowsUpdated = postRepository.decrementCommentCount(postId);
        if (rowsUpdated == 0)
            throw new ResourceConflictException("Failed to decrement comment count of the post. Possible concurrent modification or stale entity.");
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<LikeInfoDTO> getLikesOfPost(String postSlug, UUID postId, UUID likeCursor, int size) {
        UserEntity user = getCurrentUser();
        PostEntity post = postRepository.findById(postId).orElse(null);
        isInvalidPost(post, postSlug);

        if (post.getStatus() != PostStatus.PUBLISHED && !post.getUser().equals(user)) {
            throw new ResourceNotFoundException("Post not found");
        }

        return likeRepository.findLikesOfPost(postId, likeCursor, PageRequest.of(0, size));
    }

    @Override
    @Transactional
    public void likeOrDislikePost(String postSlug, UUID postId, LikeDTO likeDTO) {
        UserEntity user = getCurrentUser();

        PostEntity post = postRepository.findById(postId).orElse(null);
        isInvalidPost(post, postSlug);
        isPublishedPost(post, user);

        LikeEntity like = LikeEntity.builder()
                .user(user)
                .post(post)
                .build();

        if (likeDTO.getLike()) {
            if (likeRepository.findByUserIdAndPostId(user.getId(), postId).isEmpty()) {
                likeRepository.saveAndFlush(like);
                int rowsUpdated = postRepository.incrementLikeCount(postId);
                if (rowsUpdated == 0)
                    throw new ResourceConflictException("Failed to increment like count of the post. Possible concurrent modification or stale entity.");
            }
        } else {
            if (likeRepository.findByUserIdAndPostId(user.getId(), postId).isPresent()) {
                likeRepository.deleteByUserIdAndPostId(user.getId(), postId);
                int rowsUpdated = postRepository.decrementLikeCount(postId);
                if (rowsUpdated == 0)
                    throw new ResourceConflictException("Failed to decrement like count of the post. Possible concurrent modification or stale entity.");
            }
        }
    }

    @Override
    @Transactional
    public void bookmarkOrUnbookmarkPost(String postSlug, UUID postId, BookmarkDTO bookmarkDTO) {
        UserEntity user = getCurrentUser();

        PostEntity post = postRepository.findById(postId).orElse(null);
        isInvalidPost(post, postSlug);
        isPublishedPost(post, user);

        BookmarkEntity bookmark = BookmarkEntity.builder()
                .user(user)
                .post(post)
                .build();

        if (bookmarkDTO.getBookmark()) {
            if (bookmarkRepository.findByUserIdAndPostId(user.getId(), postId).isEmpty()) {
                bookmarkRepository.saveAndFlush(bookmark);
            }
        } else {
            if (bookmarkRepository.findByUserIdAndPostId(user.getId(), postId).isPresent()) {
                bookmarkRepository.deleteByUserIdAndPostId(user.getId(), postId);
            }
        }
    }
}
