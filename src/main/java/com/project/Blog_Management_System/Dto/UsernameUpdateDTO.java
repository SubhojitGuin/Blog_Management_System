package com.project.Blog_Management_System.Dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UsernameUpdateDTO {
    @NotNull
    @Pattern(
            regexp = "^\\w{3,30}$",
            message = "{validation.user.username}"
    )
    private String username;
}
