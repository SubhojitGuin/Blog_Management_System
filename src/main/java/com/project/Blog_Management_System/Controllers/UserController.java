package com.project.Blog_Management_System.Controllers;

import com.project.Blog_Management_System.Constants.ApiRoutes;
import com.project.Blog_Management_System.Dto.*;
import com.project.Blog_Management_System.Service.Interfaces.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.project.Blog_Management_System.Security.SecurityUtils.clearAuthCookie;


@RestController
@RequestMapping(ApiRoutes.USERS_BASE_PATH)
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "User Information & Handling", description = "Perform all user related operations")
public class UserController {

    private final UserService userService;

    @PutMapping
    @Operation(summary = "Update User Profile", description = "updates the existing user account.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "User profile updated successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data",
                    content = @Content
            )
    })
    public ResponseEntity<ProfileUpdateDTO> updateUserProfile(@Valid @RequestBody ProfileUpdateDTO profileUpdateDTO) {
        return new ResponseEntity<>(userService.updateProfile(profileUpdateDTO), HttpStatus.OK);
    }

    @PatchMapping(ApiRoutes.USER_UPDATE_PASSWORD_PATH)
    @Operation(summary = "Update User Password", description = "updates the existing user password.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "User password updated successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid old password",
                    content = @Content
            )
    })
    public ResponseEntity<Void> updateUserPassword(@Valid @RequestBody PasswordUpdateDTO passwordUpdateDTO, HttpServletResponse httpServletResponse) {
        userService.updatePassword(passwordUpdateDTO);
        httpServletResponse.addCookie(clearAuthCookie());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(ApiRoutes.USER_UPDATE_USERNAME_PATH)
    @Operation(summary = "Update User Name", description = "updates the existing user name.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "User name updated successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Username already exists",
                    content = @Content
            )
    })
    public ResponseEntity<Void> updateUserName(@Valid @RequestBody UsernameUpdateDTO usernameUpdateDTO, HttpServletResponse httpServletResponse) {
        userService.updateUserName(usernameUpdateDTO);
        httpServletResponse.addCookie(clearAuthCookie());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(ApiRoutes.USER_UPDATE_EMAIL_PATH)
    @Operation(summary = "Update User Email", description = "updates the existing user email.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "User email updated successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Email already exists",
                    content = @Content
            )
    })
    public ResponseEntity<Void> updateEmail(@Valid @RequestBody EmailUpdateDTO emailUpdateDTO, HttpServletResponse httpServletResponse) {
        userService.updateEmail(emailUpdateDTO);
        httpServletResponse.addCookie(clearAuthCookie());
        return ResponseEntity.noContent().build();
    }

    @GetMapping(ApiRoutes.USER_PATH_VARIABLE)
    @Operation(summary = "Get User Profile", description = "get user profile by username and id.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Success"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content
            )
    })
    public ResponseEntity<UserDTO> getUserProfile(@PathVariable String username,
                                                  @PathVariable UUID user_id) {
        return new ResponseEntity<>(userService.getUserProfile(username, user_id), HttpStatus.OK);
    }

    @GetMapping(ApiRoutes.USER_SEARCH_PATH)
    @Operation(summary = "Search Users", description = "search users by username or email.")
    @ApiResponse(
            responseCode = "200",
            description = "Success"
    )
    public ResponseEntity<List<UserInfoDTO>> searchUsers(@RequestParam String query) {
        return new ResponseEntity<>(userService.searchUsers(query), HttpStatus.OK);
    }

    @PostMapping(ApiRoutes.USER_FOLLOW_PATH)
    @Operation(summary = "Follow or Unfollow User", description = "follow or unfollow a user by username and id. Set 'follow' field to `true` for follow and `false` for unfollow in the request body.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "User followed/unfollowed successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data.",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "406",
                    description = "Cannot follow/unfollow yourself",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Possible concurrent modification or stale entity",
                    content = @Content
            ),
    })
    public ResponseEntity<Void> followOrUnfollowUser(@PathVariable String username,
                                                     @PathVariable UUID user_id,
                                                     @Valid @RequestBody FollowDTO followDTO) {
        userService.followOrUnfollowUser(username, user_id, followDTO);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(ApiRoutes.USER_FOLLOWERS_PATH)
    @Operation(summary = "Get Followers", description = "get followers of a user by username and id.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Success"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content
            )
    })
    public ResponseEntity<Slice<FollowInfoDTO>> getFollowers(@PathVariable String username,
                                                           @PathVariable UUID user_id,
                                                           @RequestParam(required = false) UUID follow_cursor,
                                                           @RequestParam(defaultValue = "10") int size) {
        return new ResponseEntity<>(userService.getFollowers(username, user_id, follow_cursor, size), HttpStatus.OK);
    }

    @GetMapping(ApiRoutes.USER_FOLLOWINGS_PATH)
    @Operation(summary = "Get Followings", description = "get followings of a user by username and id.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Success"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content
            )
    })
    public ResponseEntity<Slice<FollowInfoDTO>> getFollowings(@PathVariable String username,
                                                            @PathVariable UUID user_id,
                                                            @RequestParam(required = false) UUID follow_cursor,
                                                            @RequestParam(defaultValue = "10") int size) {
        return new ResponseEntity<>(userService.getFollowings(username, user_id, follow_cursor, size), HttpStatus.OK);
    }

    @DeleteMapping
    @Operation(summary = "Delete User Account", description = "Deactivate the user account, the user can login again to his account within 15 days of deactivation, after that the account will be permanently deleted.")
    @ApiResponse(
            responseCode = "204",
            description = "User Deleted Successfully"
    )
    public ResponseEntity<Void> deleteUser() {
        userService.deleteUser();
        return ResponseEntity.noContent().build();
    }

    @GetMapping(ApiRoutes.USER_POSTS_PATH)
    @Operation(summary = "Get User Posts", description = "get posts of a user by username and id.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Success"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content
            )
    })
    public ResponseEntity<Slice<PostInfoDTO>> getUserPosts(@PathVariable String username,
                                                           @PathVariable UUID user_id,
                                                           @RequestParam(required = false) UUID post_cursor,
                                                           @RequestParam(defaultValue = "10") int size) {
        return new ResponseEntity<>(userService.getUserPosts(username, user_id, post_cursor, size), HttpStatus.OK);
    }

    @GetMapping(ApiRoutes.USER_BOOKMARKS_PATH)
    @Operation(summary = "Get User Bookmarks", description = "get bookmarks of current user")
    @ApiResponse(
            responseCode = "200",
            description = "Success"
    )
    public ResponseEntity<Slice<BookmarkInfoDTO>> getUserBookmarks(@RequestParam(required = false) UUID bookmark_cursor,
                                                                   @RequestParam(defaultValue = "10") int size) {
        return new ResponseEntity<>(userService.getUserBookmarks(bookmark_cursor, size), HttpStatus.OK);
    }

}
