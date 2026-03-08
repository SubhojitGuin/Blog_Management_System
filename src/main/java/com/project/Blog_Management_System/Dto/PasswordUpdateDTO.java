package com.project.Blog_Management_System.Dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PasswordUpdateDTO {
    @NotNull
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&-+=()])(?=\\S+$).{8,20}$",
            message = "Password must be 8-20 characters long, contain at least one digit, one lowercase letter, one uppercase letter, one special character, and have no whitespace"
    )
    private String oldPassword;

    @NotNull
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&-+=()])(?=\\S+$).{8,20}$",
            message = "Password must be 8-20 characters long, contain at least one digit, one lowercase letter, one uppercase letter, one special character, and have no whitespace"
    )
    private String newPassword;
}
