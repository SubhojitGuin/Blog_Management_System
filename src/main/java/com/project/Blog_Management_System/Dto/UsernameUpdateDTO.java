package com.project.Blog_Management_System.Dto;

import com.project.Blog_Management_System.Constants.RegexConstants;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UsernameUpdateDTO {
    @NotNull
    @Pattern(
            regexp = RegexConstants.USERNAME,
            message = "{validation.user.username}"
    )
    private String username;
}
