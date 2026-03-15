package com.project.Blog_Management_System.Service.Interfaces;

import com.project.Blog_Management_System.Dto.*;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.UUID;

public interface PostService {

    PostResponseDTO createPost(PostRequestDTO postRequestDTO);

    Slice<PostResponseDTO> getAllPosts(int page, int size);

    Slice<PostResponseDTO> getAllPostsOfFollowings(int page, int size);

    List<PostInfoDTO> searchPosts(String query);

    PostResponseDTO getPost(String slug, UUID id);

    PostResponseDTO updatePost(String slug, UUID id, PostRequestDTO postRequestDTO);

    void deletePost(String slug, UUID id);

    Slice<CommentResponseDTO> getCommentsOfPost(String slug, UUID id, int page, int size);

    CommentResponseDTO addComment(String slug, UUID id, CommentRequestDTO commentRequestDTO);

}
