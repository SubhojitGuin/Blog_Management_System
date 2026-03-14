package com.project.Blog_Management_System.Service.Interfaces;

import com.project.Blog_Management_System.Dto.PostRequestDTO;
import com.project.Blog_Management_System.Dto.PostResponseDTO;
import org.springframework.data.domain.Slice;

public interface PostService {

    PostResponseDTO createPost(PostRequestDTO postRequestDTO);

    Slice<PostResponseDTO> getAllPosts(int page, int size);

    Slice<PostResponseDTO> getAllPostsOfFollowings(int page, int size);

}
