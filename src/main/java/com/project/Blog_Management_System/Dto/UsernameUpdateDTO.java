package com.project.Blog_Management_System.Dto;

import com.project.Blog_Management_System.Constants.RegexConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UsernameUpdateDTO {
    @NotBlank(message = "{validation.user.username.not_blank}")
    @Pattern(
            regexp = RegexConstants.USERNAME,
            message = "{validation.user.username}"
    )
    private String username;
}
