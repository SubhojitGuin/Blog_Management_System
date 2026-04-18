package com.project.Blog_Management_System.Dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.project.Blog_Management_System.Enums.Gender;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SignUpRequestDTO {
    @NotBlank
    private String name;

    @Pattern(
            regexp = "^\\w{3,}$",
            message = "Username must be at least 3 characters long and contain only letters, numbers, and underscores"
    )
    @NotBlank
    private String username;

    @Email
    @NotBlank
    private String email;

    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&-+=()])(?=\\S+$).{8,20}$",
            message = "Password must be 8-20 characters long, contain at least one digit, one lowercase letter, one uppercase letter, one special character, and have no whitespace"
    )
    @NotNull
    private String password;

    @NotBlank
    private Gender gender;

    private String bio;

    @Past(message = "Date of birth must be in the past and in the format yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
    @NotNull
    private LocalDate dateOfBirth;
}
