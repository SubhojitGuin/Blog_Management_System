package com.project.Blog_Management_System.Service;

import com.project.Blog_Management_System.Dto.PostRequestDTO;
import com.project.Blog_Management_System.Dto.PostResponseDTO;
import com.project.Blog_Management_System.Entities.CategoryEntity;
import com.project.Blog_Management_System.Entities.PostEntity;
import com.project.Blog_Management_System.Entities.UserEntity;
import com.project.Blog_Management_System.Repositories.CategoryRepository;
import com.project.Blog_Management_System.Repositories.PostRepository;
import com.project.Blog_Management_System.Service.Interfaces.PostService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.project.Blog_Management_System.Utils.AppUtils.generateSlug;
import static com.project.Blog_Management_System.Utils.AppUtils.getCurrentUser;
import static com.project.Blog_Management_System.Utils.ValidationUtils.isInvalidCategory;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final ModelMapper modelMapper;
    private final CategoryRepository categoryRepository;

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
        PostEntity savedPost = postRepository.save(post);
        return modelMapper.map(savedPost, PostResponseDTO.class);
    }

    @Override
    public Slice<PostResponseDTO> getAllPosts(int page, int size) {
        UserEntity user = getCurrentUser();
        return postRepository.findAllPosts(user, PageRequest.of(page, size));
    }

    @Override
    public Slice<PostResponseDTO> getAllPostsOfFollowings(int page, int size) {
        UserEntity user = getCurrentUser();
        return postRepository.findAllPostsOfFollowings(user, PageRequest.of(page, size));
    }

}
