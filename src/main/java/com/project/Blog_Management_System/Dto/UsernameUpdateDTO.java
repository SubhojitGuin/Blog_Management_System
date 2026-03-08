package com.project.Blog_Management_System.Dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UsernameUpdateDTO {
    @NotNull
    @Pattern(
            regexp = "^\\w{3,}$",
            message = "Username must be at least 3 characters long and contain only letters, numbers, and underscores"
    )
    private String username;
}
