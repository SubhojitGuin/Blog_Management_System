package com.project.Blog_Management_System.Service.Interfaces;

import com.project.Blog_Management_System.Dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.UUID;

public interface PostService {

    PostResponseDTO createPost(PostRequestDTO postRequestDTO);

    Slice<PostResponseDTO> getAllPosts(UUID postCursor, int size);

    Slice<PostResponseDTO> getAllPostsOfFollowings(UUID postCursor, int size);

    Page<PostInfoDTO> searchPosts(PostFilterRequestDTO postFilterRequestDTO, int page, int size, List<String> sort);

    PostResponseDTO getPost(String postSlug, UUID postId);

    PostResponseDTO updatePost(String postSlug, UUID postId, PostRequestDTO postRequestDTO);

    void deletePost(String postSlug, UUID postId);

    Slice<CommentResponseDTO> getCommentsOfPost(String postSlug, UUID postId, UUID commentCursor, int size);

    CommentResponseDTO addComment(String postSlug, UUID postId, CommentRequestDTO commentRequestDTO);

    CommentResponseDTO updateComment(String postSlug, UUID postId, UUID commentId, CommentRequestDTO commentRequestDTO);

    void deleteComment(String postSlug, UUID postId, UUID commentId);

    Slice<UserInfoDTO> getLikesOfPost(String postSlug, UUID postId, UUID userCursor, int size);

    void likeOrDislikePost(String postSlug, UUID postId, LikeDTO likeDTO);

}
