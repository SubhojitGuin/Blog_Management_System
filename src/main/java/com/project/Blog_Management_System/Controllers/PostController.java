package com.project.Blog_Management_System.Controllers;

import com.project.Blog_Management_System.Constants.ApiRoutes;
import com.project.Blog_Management_System.Dto.*;
import com.project.Blog_Management_System.Enums.PostStatus;
import com.project.Blog_Management_System.Service.Interfaces.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Post Created Successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Category not found",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Possible concurrent modification or stale entity",
                    content = @Content
            )
    })
    public ResponseEntity<PostResponseDTO> createPost(@Valid @RequestBody PostRequestDTO postRequestDTO) {
        return new ResponseEntity<>(postService.createPost(postRequestDTO), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get All Posts", description = "Retrieves a paginated list of all posts created by the authenticated user.")
    @ApiResponse(
            responseCode = "200",
            description = "Success"
    )
    public ResponseEntity<Slice<PostInfoDTO>> getAllPosts(@RequestParam(required = false) UUID post_cursor,
                                                          @RequestParam(defaultValue = "10") int size) {
        return new ResponseEntity<>(postService.getAllPosts(post_cursor, size), HttpStatus.OK);
    }

    @GetMapping(ApiRoutes.POST_FOLLOWING_PATH)
    @Operation(summary = "Get Posts of Followings", description = "Retrieves a paginated list of posts created by the users that the authenticated user is following.")
    @ApiResponse(
            responseCode = "200",
            description = "Success"
    )
    public ResponseEntity<Slice<PostInfoDTO>> getAllPostsOfFollowings(@RequestParam(required = false) UUID post_cursor,
                                                                      @RequestParam(defaultValue = "10") int size) {
        return new ResponseEntity<>(postService.getAllPostsOfFollowings(post_cursor, size), HttpStatus.OK);
    }

    @GetMapping(ApiRoutes.POST_UNPUBLISHED_PATH)
    @Operation(summary = "Get Unpublished Posts", description = "Retrieves a paginated list of all unpublished posts created by the authenticated user.")
    @ApiResponse(
            responseCode = "200",
            description = "Success"
    )
    public ResponseEntity<Slice<PostInfoDTO>> getAllUnpublishedPosts(@RequestParam PostStatus status,
                                                                    @RequestParam(required = false) UUID post_cursor,
                                                                    @RequestParam(defaultValue = "10") int size) {
        return new ResponseEntity<>(postService.getPostsByStatus(status, post_cursor, size), HttpStatus.OK);
    }

    @GetMapping(ApiRoutes.POST_SEARCH_PATH)
    @Operation(summary = "Search Posts", description = "Searches for posts based on the provided query string.")
    @ApiResponse(
            responseCode = "200",
            description = "Success"
    )
    public ResponseEntity<Page<PostInfoDTO>> searchPosts(@Valid @ModelAttribute PostFilterRequestDTO postFilterRequestDTO,
                                                         @RequestParam(required = false) List<String> sort,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "10") int size) {
        return new ResponseEntity<>(postService.searchPosts(postFilterRequestDTO, page, size, sort), HttpStatus.OK);
    }

    @GetMapping(ApiRoutes.POST_PATH_VARIABLE)
    @Operation(summary = "Get Post by ID", description = "Retrieves the details of a specific post by its slug and ID.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Success"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid filter",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Post not found with the provided slug and ID.",
                    content = @Content
            )
    })
    public ResponseEntity<PostResponseDTO> getPost(@PathVariable String post_slug,
                                                   @PathVariable UUID post_id) {
        return new ResponseEntity<>(postService.getPost(post_slug, post_id), HttpStatus.OK);
    }

    @PutMapping(ApiRoutes.POST_PATH_VARIABLE)
    @Operation(summary = "Update Post", description = "Updates the details of an existing post identified by its slug and ID.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Post updated successfully."
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data.",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Post not found with the provided slug and ID.",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "The authenticated user does not have permission to update this post.",
                    content = @Content
            )
    })
    public ResponseEntity<PostResponseDTO> updatePost(@PathVariable String post_slug,
                                                      @PathVariable UUID post_id,
                                                      @Valid @RequestBody PostRequestDTO postRequestDTO) {
        return new ResponseEntity<>(postService.updatePost(post_slug, post_id, postRequestDTO), HttpStatus.OK);
    }

    @DeleteMapping(ApiRoutes.POST_PATH_VARIABLE)
    @Operation(summary = "Delete Post", description = "Delete an existing post identified by its slug and ID.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Post deleted successfully."
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Post not found with the provided slug and ID.",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "The authenticated user does not have permission to delete this post.",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Possible concurrent modification or stale entity.",
                    content = @Content
            )
    })
    public ResponseEntity<Void> deletePost(@PathVariable String post_slug,
                                           @PathVariable UUID post_id) {
        postService.deletePost(post_slug, post_id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(ApiRoutes.POST_COMMENTS_PATH)
    @Operation(summary = "Get Top level comments of the post", description = "Get all the comments of a post identified by its slug and ID.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Success"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Post not found with the provided slug and ID.",
                    content = @Content
            )
    })
    public ResponseEntity<Slice<CommentResponseDTO>> findTopLevelCommentsOfPost(@PathVariable String post_slug,
                                                                                @PathVariable UUID post_id,
                                                                                @RequestParam(required = false) UUID comment_cursor,
                                                                                @RequestParam(defaultValue = "10") int size) {
        return new ResponseEntity<>(postService.getTopLevelCommentsOfPost(post_slug, post_id, comment_cursor, size), HttpStatus.OK);
    }

    @GetMapping(ApiRoutes.POST_COMMENT_REPLIES_PATH)
    @Operation(summary = "Get replies to Top level comments of the post", description = "Get the replies to top level comments of a post identified by its slug and ID.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Success"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Post or Comment not found with the provided slug and ID.",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Parent Comment post mismatch",
                    content = @Content
            ),
    })
    public ResponseEntity<Slice<CommentResponseDTO>> findRepliesOfComment(@PathVariable String post_slug,
                                                                          @PathVariable UUID post_id,
                                                                          @PathVariable UUID comment_id,
                                                                          @RequestParam(required = false) UUID comment_cursor,
                                                                          @RequestParam(defaultValue = "10") int size) {
        return new ResponseEntity<>(postService.getRepliesOfComment(post_slug, post_id, comment_id, comment_cursor, size), HttpStatus.OK);
    }

    @PostMapping(ApiRoutes.POST_COMMENTS_PATH)
    @Operation(summary = "Add Top level comment to the post", description = "Add a top level comment to a post identified by its slug and ID.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Comment added successfully."
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data.",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Post not found with the provided slug and ID.",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Possible concurrent modification or stale entity",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "406",
                    description = "Unpublished post cannot be commented",
                    content = @Content
            )
    })
    public ResponseEntity<CommentResponseDTO> addTopLevelComment(@PathVariable String post_slug,
                                                                 @PathVariable UUID post_id,
                                                                 @Valid @RequestBody CommentRequestDTO commentRequestDTO) {
        return new ResponseEntity<>(postService.addTopLevelComments(post_slug, post_id, commentRequestDTO), HttpStatus.CREATED);
    }

    @PostMapping(ApiRoutes.POST_COMMENT_REPLIES_PATH)
    @Operation(summary = "Add Replies to top level comments of post", description = "Add replies to top level comment identified by its slug and ID.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Comment added successfully."
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Post or Comment not found with the provided slug and ID.",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid comment or Parent Comment post mismatch or invalid reply depth",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Possible concurrent modification or stale entity",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "406",
                    description = "Unpublished post cannot be commented",
                    content = @Content
            )
    })
    public ResponseEntity<CommentResponseDTO> addReplyToComment(@PathVariable String post_slug,
                                                                @PathVariable UUID post_id,
                                                                @PathVariable UUID comment_id,
                                                                @Valid @RequestBody CommentRequestDTO commentRequestDTO) {
        return new ResponseEntity<>(postService.addReplyToComment(post_slug, post_id, comment_id, commentRequestDTO), HttpStatus.CREATED);
    }

    @PutMapping(ApiRoutes.POST_COMMENT_PATH)
    @Operation(summary = "Update comment of the post", description = "Update a comment to a post identified by its post slug, post ID and comment ID.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Comment updated successfully."
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data.",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Post or Comment not found with the provided slug and ID.",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "The authenticated user does not have permission to update this comment.",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Possible concurrent modification or stale entity",
                    content = @Content
            )
    })
    public ResponseEntity<CommentResponseDTO> updateComment(@PathVariable String post_slug,
                                                            @PathVariable UUID post_id,
                                                            @PathVariable UUID comment_id,
                                                            @Valid @RequestBody CommentRequestDTO commentRequestDTO) {
        return new ResponseEntity<>(postService.updateComment(post_slug, post_id, comment_id, commentRequestDTO), HttpStatus.OK);
    }

    @DeleteMapping(ApiRoutes.POST_COMMENT_PATH)
    @Operation(summary = "Delete a comment of the post", description = "Delete a comment to a post identified by its post slug, post ID and comment ID.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Comment deleted successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Post or Comment not found with the provided slug and ID.",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "The authenticated user does not have permission to update this comment.",
                    content = @Content
            )
    })
    public ResponseEntity<Void> deleteComment(@PathVariable String post_slug,
                                              @PathVariable UUID post_id,
                                              @PathVariable UUID comment_id) {
        postService.deleteComment(post_slug, post_id, comment_id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(ApiRoutes.POST_LIKES_PATH)
    @Operation(summary = "Get all likes of a post", description = "Get all the liked users of a post using the post slug and post ID")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Success"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Post not found",
                    content = @Content
            )
    })
    public ResponseEntity<Slice<LikeInfoDTO>> getLikesOfPost(@PathVariable String post_slug,
                                                             @PathVariable UUID post_id,
                                                             @RequestParam(required = false) UUID like_cursor,
                                                             @RequestParam(defaultValue = "10") int size) {
        return new ResponseEntity<>(postService.getLikesOfPost(post_slug, post_id, like_cursor, size), HttpStatus.OK);
    }

    @PostMapping(ApiRoutes.POST_LIKES_PATH)
    @Operation(summary = "Like or Dislike a post", description = "Like or Dislike a post using the post slug and post ID. Set 'like' field to `true` for like and `false` for dislike in the request body.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Success"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data.",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Post not found",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "406",
                    description = "Unpublished post cannot be liked",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Possible concurrent modification or stale entity",
                    content = @Content
            )
    })
    public ResponseEntity<Void> likeOrDislikePost(@PathVariable String post_slug,
                                                  @PathVariable UUID post_id,
                                                  @Valid @RequestBody LikeDTO likeDTO) {
        postService.likeOrDislikePost(post_slug, post_id, likeDTO);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(ApiRoutes.POST_BOOKMARK_PATH)
    @Operation(summary = "Bookmark/Unbookmark a post", description = "Bookmark/Unbookmark a post using the post slug and post ID. Set 'bookmark' field to `true` for bookmark and `false` for unbookmark in the request body.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Success"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data.",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Post not found",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "406",
                    description = "Unpublished post cannot be bookmarked",
                    content = @Content
            )
    })
    public ResponseEntity<Void> bookmarkOrUnbookmarkPost(@PathVariable String post_slug,
                                                         @PathVariable UUID post_id,
                                                         @Valid @RequestBody BookmarkDTO bookmarkDTO) {
        postService.bookmarkOrUnbookmarkPost(post_slug, post_id, bookmarkDTO);
        return ResponseEntity.noContent().build();
    }
}
