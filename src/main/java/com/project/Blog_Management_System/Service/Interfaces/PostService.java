package com.project.Blog_Management_System.Service.Interfaces;

import com.project.Blog_Management_System.Dto.*;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.UUID;

public interface PostService {

    PostResponseDTO createPost(PostRequestDTO postRequestDTO);

    Slice<PostResponseDTO> getAllPosts(int page, int size);

    Slice<PostResponseDTO> getAllPostsOfFollowings(int page, int size);

    Slice<PostInfoDTO> searchPosts(PostFilterRequestDTO postFilterRequestDTO, int page, int size);

    PostResponseDTO getPost(String slug, UUID id);

    PostResponseDTO updatePost(String slug, UUID id, PostRequestDTO postRequestDTO);

    void deletePost(String slug, UUID id);

    Slice<CommentResponseDTO> getCommentsOfPost(String slug, UUID id, int page, int size);

    CommentResponseDTO addComment(String slug, UUID id, CommentRequestDTO commentRequestDTO);

    CommentResponseDTO updateComment(String slug, UUID post_id, UUID comment_id, CommentRequestDTO commentRequestDTO);

    void deleteComment(String slug, UUID post_id, UUID comment_id);

    Slice<UserInfoDTO> getLikesOfPost(String slug, UUID id, int page, int size);

    void likeOrDislikePost(String slug, UUID id, LikeDTO likeDTO);

}
