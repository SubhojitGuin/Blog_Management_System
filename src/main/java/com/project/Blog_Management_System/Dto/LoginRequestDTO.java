package com.project.Blog_Management_System.Dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class LoginRequestDTO {
    @Pattern(
            regexp = "(^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$)|(^\\w{3,30}$)",
            message = "{validation.user.email_or_username}"
    )
    @NotNull(message = "{validation.user.email_or_username.not_null}")
    private String emailOrUsername;

    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&-+=()])(?=\\S+$).{8,20}$",
            message = "{validation.user.password}"
    )
    @NotNull(message = "{validation.user.password.not_null}")
    private String password;
}
