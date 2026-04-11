package com.project.Blog_Management_System.Service;

import com.project.Blog_Management_System.Dto.*;
import com.project.Blog_Management_System.Entities.*;
import com.project.Blog_Management_System.Enums.Role;
import com.project.Blog_Management_System.Repositories.CategoryRepository;
import com.project.Blog_Management_System.Repositories.CommentRepository;
import com.project.Blog_Management_System.Repositories.LikeRepository;
import com.project.Blog_Management_System.Repositories.PostRepository;
import com.project.Blog_Management_System.Service.Interfaces.PostService;
import com.project.Blog_Management_System.Specifications.PostFilterSpecification;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        PostEntity savedPost = postRepository.saveAndFlush(post);

        return modelMapper.map(savedPost, PostResponseDTO.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<PostResponseDTO> getAllPosts(int page, int size) {
        UserEntity user = getCurrentUser();
        return postRepository.findAllPosts(user, PageRequest.of(page, size));
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<PostResponseDTO> getAllPostsOfFollowings(int page, int size) {
        UserEntity user = getCurrentUser();
        return postRepository.findAllPostsOfFollowings(user, PageRequest.of(page, size));
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<PostInfoDTO> searchPosts(PostFilterRequestDTO postFilterRequestDTO, int page, int size) {
        Specification<PostEntity> spec = PostFilterSpecification.buildSpecification(postFilterRequestDTO);
        return postRepository.findAll(spec, PageRequest.of(page, size))
                .map(post -> modelMapper.map(post, PostInfoDTO.class));
    }

    @Override
    @Transactional(readOnly = true)
    public PostResponseDTO getPost(String slug, UUID id) {
        UserEntity user = getCurrentUser();

        PostEntity post = postRepository.findById(id).orElse(null);
        isInvalidPost(post, slug);

        PostResponseDTO postResponseDTO = modelMapper.map(post, PostResponseDTO.class);
        postResponseDTO.setIsOwner(user.equals(post.getUser()));
        postResponseDTO.setIsLiked(likeRepository.findByUserAndPost(user, post).isPresent());

        return postResponseDTO;
    }

    @Override
    @Transactional
    public PostResponseDTO updatePost(String slug, UUID id, PostRequestDTO postRequestDTO) {
        UserEntity user = getCurrentUser();

        PostEntity post = postRepository.findById(id).orElse(null);
        isInvalidPost(post, slug);

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

        postRepository.saveAndFlush(post);

        return modelMapper.map(post, PostResponseDTO.class);
    }

    @Override
    @Transactional
    public void deletePost(String slug, UUID id) {
        UserEntity user = getCurrentUser();

        PostEntity post = postRepository.findById(id).orElse(null);
        isInvalidPost(post, slug);

        if (!post.getUser().equals(user) && !hasRole(Role.ADMIN)) {
            throw new AccessDeniedException("You are not authorized to delete this post");
        }

        postRepository.delete(post);
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<CommentResponseDTO> getCommentsOfPost(String slug, UUID id, int page, int size) {
        UserEntity user = getCurrentUser();
        PostEntity post = postRepository.findById(id).orElse(null);
        isInvalidPost(post, slug);

        return commentRepository.findByPost(post, user, PageRequest.of(page, size));
    }

    @Override
    @Transactional
    public CommentResponseDTO addComment(String slug, UUID id, CommentRequestDTO commentRequestDTO) {
        UserEntity user = getCurrentUser();

        PostEntity post = postRepository.findById(id).orElse(null);
        isInvalidPost(post, slug);

        CommentEntity comment = modelMapper.map(commentRequestDTO, CommentEntity.class);
        comment.setUser(user);
        comment.setPost(post);
        CommentEntity savedComment = commentRepository.saveAndFlush(comment);

        return modelMapper.map(savedComment, CommentResponseDTO.class);
    }

    @Override
    @Transactional
    public CommentResponseDTO updateComment(String slug, UUID post_id, UUID comment_id, CommentRequestDTO commentRequestDTO) {
        UserEntity user = getCurrentUser();

        PostEntity post = postRepository.findById(post_id).orElse(null);
        isInvalidPost(post, slug);

        CommentEntity comment = commentRepository.findById(comment_id).orElse(null);
        isInvalidComment(comment);

        if (!comment.getUser().equals(user)) {
            throw new AccessDeniedException("You are not authorized to update this comment");
        }

        modelMapper.map(commentRequestDTO, comment);
        commentRepository.saveAndFlush(comment);
        return modelMapper.map(comment, CommentResponseDTO.class);
    }

    @Override
    @Transactional
    public void deleteComment(String slug, UUID post_id, UUID comment_id) {
        UserEntity user = getCurrentUser();

        PostEntity post = postRepository.findById(post_id).orElse(null);
        isInvalidPost(post, slug);

        CommentEntity comment = commentRepository.findById(comment_id).orElse(null);
        isInvalidComment(comment);

        if (!comment.getUser().equals(user) && !hasRole(Role.ADMIN)) {
            throw new AccessDeniedException("You are not authorized to delete this comment");
        }

        commentRepository.delete(comment);
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<UserInfoDTO> getLikesOfPost(String slug, UUID id, int page, int size) {
        PostEntity post = postRepository.findById(id).orElse(null);
        isInvalidPost(post, slug);

        return likeRepository.findLikesOfPost(post, PageRequest.of(page, size));
    }

    @Override
    @Transactional
    public void likeOrDislikePost(String slug, UUID id, LikeDTO likeDTO) {
        UserEntity user = getCurrentUser();

        PostEntity post = postRepository.findById(id).orElse(null);
        isInvalidPost(post, slug);

        LikeEntity like = LikeEntity.builder()
                .user(user)
                .post(post)
                .build();

        if (likeDTO.getLike()) {
            if (likeRepository.findByUserAndPost(user, post).isEmpty()) {
                likeRepository.saveAndFlush(like);
            }
        } else {
            if (likeRepository.findByUserAndPost(user, post).isPresent()) {
                likeRepository.deleteByUserAndPost(user, post);
            }
        }
    }

}
