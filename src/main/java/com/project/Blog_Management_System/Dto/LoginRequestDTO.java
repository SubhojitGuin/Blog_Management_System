package com.project.Blog_Management_System.Dto;

import com.project.Blog_Management_System.Constants.RegexConstants;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class LoginRequestDTO {
    @Pattern(
            regexp = RegexConstants.EMAIL_OR_USERNAME,
            message = "{validation.user.email_or_username}"
    )
    @NotNull(message = "{validation.user.email_or_username.not_null}")
    private String emailOrUsername;

    @Pattern(
            regexp = RegexConstants.PASSWORD,
            message = "{validation.user.password}"
    )
    @NotNull(message = "{validation.user.password.not_null}")
    private String password;
}
