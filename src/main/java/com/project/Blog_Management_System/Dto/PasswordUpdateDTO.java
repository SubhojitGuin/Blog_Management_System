package com.project.Blog_Management_System.Dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PasswordUpdateDTO {
    @NotNull(message = "{validation.user.password.not_null}")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&-+=()])(?=\\S+$).{8,20}$",
            message = "{validation.user.password}"
    )
    private String oldPassword;

    @NotNull(message = "{validation.user.password.not_null}")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&-+=()])(?=\\S+$).{8,20}$",
            message = "{validation.user.password}"
    )
    private String newPassword;
}
