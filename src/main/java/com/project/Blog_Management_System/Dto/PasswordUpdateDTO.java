package com.project.Blog_Management_System.Dto;

import com.project.Blog_Management_System.Constants.RegexConstants;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordUpdateDTO {
    @NotNull(message = "{validation.user.password.not_null}")
    @Pattern(regexp = RegexConstants.PASSWORD,
            message = "{validation.user.password}"
    )
    private String oldPassword;

    @NotNull(message = "{validation.user.password.not_null}")
    @Pattern(regexp = RegexConstants.PASSWORD,
            message = "{validation.user.password}"
    )
    private String newPassword;
}
