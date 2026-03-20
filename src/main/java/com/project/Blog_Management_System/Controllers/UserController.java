package com.project.Blog_Management_System.Controllers;

import com.project.Blog_Management_System.Dto.*;
import com.project.Blog_Management_System.Service.Interfaces.UserService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "User Information & Handling", description = "Perform all user related operations")
public class UserController {

    private final UserService userService;

    @PutMapping
    @Operation(summary = "Update User Profile", description = "updates the existing user account.")
    public ResponseEntity<ProfileUpdateDTO> updateUserProfile(@Valid @RequestBody ProfileUpdateDTO profileUpdateDTO) {
        return new ResponseEntity<>(userService.updateProfile(profileUpdateDTO), HttpStatus.OK);
    }

    @PatchMapping("/update_password")
    @Operation(summary = "Update User Password", description = "updates the existing user password.")
    public ResponseEntity<Void> updateUserPassword(@Valid @RequestBody PasswordUpdateDTO passwordUpdateDTO, HttpServletResponse httpServletResponse) {
        userService.updatePassword(passwordUpdateDTO);
        httpServletResponse.addCookie(clearAuthCookie());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/update_username")
    @Operation(summary = "Update User Name", description = "updates the existing user name.")
    public ResponseEntity<Void> updateUserName(@Valid @RequestBody UsernameUpdateDTO usernameUpdateDTO, HttpServletResponse httpServletResponse) {
        userService.updateUserName(usernameUpdateDTO);
        httpServletResponse.addCookie(clearAuthCookie());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/update_email")
    @Operation(summary = "Update User Email", description = "updates the existing user email.")
    public ResponseEntity<Void> updateEmail(@Valid @RequestBody EmailUpdateDTO emailUpdateDTO, HttpServletResponse httpServletResponse) {
        userService.updateEmail(emailUpdateDTO);
        httpServletResponse.addCookie(clearAuthCookie());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{username}-{id:[0-9a-fA-F\\-]{36}}")
    public ResponseEntity<UserDTO> getUserProfile(@PathVariable String username,
                                                  @PathVariable UUID id) {
        return new ResponseEntity<>(userService.getUserProfile(username, id), HttpStatus.OK);
    }

    @GetMapping("/search")
    @Operation(summary = "Search Users", description = "search users by username or email.")
    public ResponseEntity<List<UserInfoDTO>> searchUsers(@RequestParam String query) {
        return new ResponseEntity<>(userService.searchUsers(query), HttpStatus.OK);
    }

    @PostMapping("/{username}-{id:[0-9a-fA-F\\-]{36}}/follow")
    @Operation(summary = "Follow or Unfollow User", description = "follow or unfollow a user by username and id.")
    public ResponseEntity<Void> followOrUnfollowUser(@PathVariable String username,
                                                     @PathVariable UUID id,
                                                     @RequestBody FollowDTO followDTO) {
        userService.followOrUnfollowUser(username, id, followDTO);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{username}-{id:[0-9a-fA-F\\-]{36}}/followers")
    @Operation(summary = "Get Followers", description = "get followers of a user by username and id.")
    public ResponseEntity<Slice<UserInfoDTO>> getFollowers(@PathVariable String username,
                                                           @PathVariable UUID id,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "10") int size) {
        return new ResponseEntity<>(userService.getFollowers(username, id, page, size), HttpStatus.OK);
    }

    @GetMapping("/{username}-{id:[0-9a-fA-F\\-]{36}}/followings")
    @Operation(summary = "Get Followings", description = "get followings of a user by username and id.")
    public ResponseEntity<Slice<UserInfoDTO>> getFollowings(@PathVariable String username,
                                                            @PathVariable UUID id,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "10") int size) {
        return new ResponseEntity<>(userService.getFollowings(username, id, page, size), HttpStatus.OK);
    }

    @DeleteMapping
    @Operation(summary = "Delete User Account", description = "Deactivate the user account, the user can login again to his account within 15 days of deactivation, after that the account will be permanently deleted.")
    public ResponseEntity<Void> deleteUser() {
        userService.deleteUser();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{username}-{id:[0-9a-fA-F\\-]{36}}/posts")
    @Operation(summary = "Get User Posts", description = "get posts of a user by username and id.")
    public ResponseEntity<Slice<PostResponseDTO>> getUserPosts(@PathVariable String username,
                                                              @PathVariable UUID id,
                                                              @RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "10") int size) {
        return new ResponseEntity<>(userService.getUserPosts(username, id, page, size), HttpStatus.OK);
    }

}
