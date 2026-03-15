package com.project.Blog_Management_System.Controllers;

import com.project.Blog_Management_System.Dto.*;
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

import java.util.List;
import java.util.UUID;

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

    @GetMapping("/search")
    @Operation(summary = "Search Posts", description = "Searches for posts based on the provided query string.")
    public ResponseEntity<List<PostInfoDTO>> searchPosts(@RequestParam String query) {
        return new ResponseEntity<>(postService.searchPosts(query), HttpStatus.OK);
    }

    @GetMapping("/{slug}-{id:[0-9a-fA-F\\-]{36}}")
    @Operation(summary = "Get Post by ID", description = "Retrieves the details of a specific post by its slug and ID.")
    public ResponseEntity<PostResponseDTO> getPost(@PathVariable String slug,
                                                   @PathVariable UUID id) {
        return new ResponseEntity<>(postService.getPost(slug, id), HttpStatus.OK);
    }

    @PutMapping("/{slug}-{id:[0-9a-fA-F\\-]{36}}")
    @Operation(summary = "Update Post", description = "Updates the details of an existing post identified by its slug and ID.")
    public ResponseEntity<PostResponseDTO> updatePost(@PathVariable String slug,
                                                      @PathVariable UUID id,
                                                      @Valid @RequestBody PostRequestDTO postRequestDTO) {
        return new ResponseEntity<>(postService.updatePost(slug, id, postRequestDTO), HttpStatus.OK);
    }

    @DeleteMapping("/{slug}-{id:[0-9a-fA-F\\-]{36}}")
    @Operation(summary = "Delete Post", description = "Delete an existing post identified by its slug and ID.")
    public ResponseEntity<Void> deletePost(@PathVariable String slug,
                                           @PathVariable UUID id) {
        postService.deletePost(slug, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{slug}-{id:[0-9a-fA-F\\-]{36}}/comments")
    @Operation(summary = "Get comments of the post", description = "Get all the comments of a post identified by its slug and ID.")
    public ResponseEntity<Slice<CommentResponseDTO>> findCommentsOfPost(@PathVariable String slug,
                                                                        @PathVariable UUID id,
                                                                        @RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = "10") int size) {
        return new ResponseEntity<>(postService.getCommentsOfPost(slug, id, page, size), HttpStatus.OK);
    }

    @PostMapping("/{slug}-{id:[0-9a-fA-F\\-]{36}}/comments")
    @Operation(summary = "Add comment to the post", description = "Add a comment to a post identified by its slug and ID.")
    public ResponseEntity<CommentResponseDTO> addComment(@PathVariable String slug,
                                                         @PathVariable UUID id,
                                                         @Valid @RequestBody CommentRequestDTO commentRequestDTO) {
        return new ResponseEntity<>(postService.addComment(slug, id, commentRequestDTO), HttpStatus.CREATED);
    }

}
