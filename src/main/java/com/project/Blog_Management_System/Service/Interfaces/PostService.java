package com.project.Blog_Management_System.Service.Interfaces;

import com.project.Blog_Management_System.Dto.*;
import com.project.Blog_Management_System.Enums.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.UUID;

public interface PostService {

    PostResponseDTO createPost(PostRequestDTO postRequestDTO);

    Slice<PostInfoDTO> getAllPosts(UUID postCursor, int size);

    Slice<PostInfoDTO> getAllPostsOfFollowings(UUID postCursor, int size);

    Page<PostInfoDTO> searchPosts(PostFilterRequestDTO postFilterRequestDTO, int page, int size, List<String> sort);

    Slice<PostInfoDTO> getPostsByStatus(PostStatus status, UUID postCursor, int size);

    PostResponseDTO getPost(String postSlug, UUID postId);

    PostResponseDTO updatePost(String postSlug, UUID postId, PostRequestDTO postRequestDTO);

    void deletePost(String postSlug, UUID postId);

    Slice<CommentResponseDTO> getCommentsOfPost(String postSlug, UUID postId, UUID commentCursor, int size);

    CommentResponseDTO addComment(String postSlug, UUID postId, CommentRequestDTO commentRequestDTO);

    CommentResponseDTO updateComment(String postSlug, UUID postId, UUID commentId, CommentRequestDTO commentRequestDTO);

    void deleteComment(String postSlug, UUID postId, UUID commentId);

    Slice<LikeInfoDTO> getLikesOfPost(String postSlug, UUID postId, UUID likeCursor, int size);

    void likeOrDislikePost(String postSlug, UUID postId, LikeDTO likeDTO);

    void bookmarkOrUnbookmarkPost(String postSlug, UUID postId, BookmarkDTO bookmarkDTO);

}
