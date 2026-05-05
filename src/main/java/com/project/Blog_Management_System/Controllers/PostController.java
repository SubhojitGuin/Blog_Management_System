package com.project.Blog_Management_System.Controllers;

import com.project.Blog_Management_System.Constants.ApiRoutes;
import com.project.Blog_Management_System.Dto.*;
import com.project.Blog_Management_System.Service.Interfaces.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiRoutes.POSTS_BASE_PATH)
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
    public ResponseEntity<Slice<PostResponseDTO>> getAllPosts(@RequestParam(required = false) UUID post_cursor,
                                                              @RequestParam(defaultValue = "10") int size) {
        return new ResponseEntity<>(postService.getAllPosts(post_cursor, size), HttpStatus.OK);
    }

    @GetMapping(ApiRoutes.POST_FOLLOWING_PATH)
    @Operation(summary = "Get Posts of Followings", description = "Retrieves a paginated list of posts created by the users that the authenticated user is following.")
    public ResponseEntity<Slice<PostResponseDTO>> getAllPostsOfFollowings(@RequestParam(required = false) UUID post_cursor,
                                                                          @RequestParam(defaultValue = "10") int size) {
        return new ResponseEntity<>(postService.getAllPostsOfFollowings(post_cursor, size), HttpStatus.OK);
    }

    @GetMapping(ApiRoutes.POST_SEARCH_PATH)
    @Operation(summary = "Search Posts", description = "Searches for posts based on the provided query string.")
    public ResponseEntity<Page<PostInfoDTO>> searchPosts(@ModelAttribute PostFilterRequestDTO postFilterRequestDTO,
                                                         @RequestParam(required = false) List<String> sort,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "10") int size) {
        return new ResponseEntity<>(postService.searchPosts(postFilterRequestDTO, page, size, sort), HttpStatus.OK);
    }

    @GetMapping(ApiRoutes.POST_PATH_VARIABLE)
    @Operation(summary = "Get Post by ID", description = "Retrieves the details of a specific post by its slug and ID.")
    public ResponseEntity<PostResponseDTO> getPost(@PathVariable String post_slug,
                                                   @PathVariable UUID post_id) {
        return new ResponseEntity<>(postService.getPost(post_slug, post_id), HttpStatus.OK);
    }

    @PutMapping(ApiRoutes.POST_PATH_VARIABLE)
    @Operation(summary = "Update Post", description = "Updates the details of an existing post identified by its slug and ID.")
    public ResponseEntity<PostResponseDTO> updatePost(@PathVariable String post_slug,
                                                      @PathVariable UUID post_id,
                                                      @Valid @RequestBody PostRequestDTO postRequestDTO) {
        return new ResponseEntity<>(postService.updatePost(post_slug, post_id, postRequestDTO), HttpStatus.OK);
    }

    @DeleteMapping(ApiRoutes.POST_PATH_VARIABLE)
    @Operation(summary = "Delete Post", description = "Delete an existing post identified by its slug and ID.")
    public ResponseEntity<Void> deletePost(@PathVariable String post_slug,
                                           @PathVariable UUID post_id) {
        postService.deletePost(post_slug, post_id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(ApiRoutes.POST_COMMENTS_PATH)
    @Operation(summary = "Get comments of the post", description = "Get all the comments of a post identified by its slug and ID.")
    public ResponseEntity<Slice<CommentResponseDTO>> findCommentsOfPost(@PathVariable String post_slug,
                                                                        @PathVariable UUID post_id,
                                                                        @RequestParam(required = false) UUID comment_cursor,
                                                                        @RequestParam(defaultValue = "10") int size) {
        return new ResponseEntity<>(postService.getCommentsOfPost(post_slug, post_id, comment_cursor, size), HttpStatus.OK);
    }

    @PostMapping(ApiRoutes.POST_COMMENTS_PATH)
    @Operation(summary = "Add comment to the post", description = "Add a comment to a post identified by its slug and ID.")
    public ResponseEntity<CommentResponseDTO> addComment(@PathVariable String post_slug,
                                                         @PathVariable UUID post_id,
                                                         @Valid @RequestBody CommentRequestDTO commentRequestDTO) {
        return new ResponseEntity<>(postService.addComment(post_slug, post_id, commentRequestDTO), HttpStatus.CREATED);
    }

    @PutMapping(ApiRoutes.POST_COMMENT_PATH)
    @Operation(summary = "Update comment of the post", description = "Update a comment to a post identified by its post slug, post ID and comment ID.")
    public ResponseEntity<CommentResponseDTO> updateComment(@PathVariable String post_slug,
                                                            @PathVariable UUID post_id,
                                                            @PathVariable UUID comment_id,
                                                            @Valid @RequestBody CommentRequestDTO commentRequestDTO) {
        return new ResponseEntity<>(postService.updateComment(post_slug, post_id, comment_id, commentRequestDTO), HttpStatus.OK);
    }

    @DeleteMapping(ApiRoutes.POST_COMMENT_PATH)
    @Operation(summary = "Delete a comment of the post", description = "Delete a comment to a post identified by its post slug, post ID and comment ID.")
    public ResponseEntity<Void> deleteComment(@PathVariable String post_slug,
                                              @PathVariable UUID post_id,
                                              @PathVariable UUID comment_id) {
        postService.deleteComment(post_slug, post_id, comment_id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(ApiRoutes.POST_LIKES_PATH)
    @Operation(summary = "Get all likes of a post", description = "Get all the liked users of a post using the post slug and post ID")
    public ResponseEntity<Slice<LikeInfoDTO>> getLikesOfPost(@PathVariable String post_slug,
                                                             @PathVariable UUID post_id,
                                                             @RequestParam(required = false) UUID like_cursor,
                                                             @RequestParam(defaultValue = "10") int size) {
        return new ResponseEntity<>(postService.getLikesOfPost(post_slug, post_id, like_cursor, size), HttpStatus.OK);
    }

    @PostMapping(ApiRoutes.POST_LIKES_PATH)
    @Operation(summary = "Like or Dislike a post", description = "Like or Dislike a post using the post slug and post ID. Set 'like' field to `true` for like and `false` for dislike in the request body.")
    public ResponseEntity<Void> likeOrDislikePost(@PathVariable String post_slug,
                                                  @PathVariable UUID post_id,
                                                  @Valid @RequestBody LikeDTO likeDTO) {
        postService.likeOrDislikePost(post_slug, post_id, likeDTO);
        return ResponseEntity.noContent().build();
    }

}
