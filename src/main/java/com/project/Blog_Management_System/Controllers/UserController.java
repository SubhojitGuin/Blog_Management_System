package com.project.Blog_Management_System.Controllers;

import com.project.Blog_Management_System.Dto.ProfileUpdateDTO;
import com.project.Blog_Management_System.Service.Interfaces.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
