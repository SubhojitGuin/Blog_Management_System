package com.project.Blog_Management_System.Controllers;

import com.project.Blog_Management_System.Dto.PostRequestDTO;
import com.project.Blog_Management_System.Dto.PostResponseDTO;
import com.project.Blog_Management_System.Service.Interfaces.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Post Information & Handling", description = "Perform all post related operations")
public class PostController {

    private final PostService postService;

    @PostMapping
    @Operation(summary = "Create a New Post", description = "Creates a new post with the provided details.")
    public ResponseEntity<PostResponseDTO> createPost(@Valid @RequestBody PostRequestDTO postRequestDTO) {
        return new ResponseEntity<>(postService.createPost(postRequestDTO), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get All Posts", description = "Retrieves a paginated list of all posts created by the authenticated user.")
    public ResponseEntity<Slice<PostResponseDTO>> getAllPosts(@RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "10") int size) {
        return new ResponseEntity<>(postService.getAllPosts(page, size), HttpStatus.OK);
    }

    @GetMapping("/following")
    @Operation(summary = "Get Posts of Followings", description = "Retrieves a paginated list of posts created by the users that the authenticated user is following.")
    public ResponseEntity<Slice<PostResponseDTO>> getAllPostsOfFollowings(@RequestParam(defaultValue = "0") int page,
                                                                          @RequestParam(defaultValue = "10") int size) {
        return new ResponseEntity<>(postService.getAllPostsOfFollowings(page, size), HttpStatus.OK);
    }
}
