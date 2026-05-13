package com.project.Blog_Management_System.Dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.project.Blog_Management_System.Deserializers.BasicHtmlSanitizationDeserializer;
import com.project.Blog_Management_System.Deserializers.StringSanitizationDeserializer;
import com.project.Blog_Management_System.Enums.Gender;
import jakarta.validation.constraints.*;
import lombok.Data;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.time.LocalDate;

@Data
public class SignUpRequestDTO {
    @NotBlank(message = "{validation.user.name.not_blank}")
    @Size(min = 2, max = 255, message = "{validation.user.name.size}")
    @JsonDeserialize(using = StringSanitizationDeserializer.class)
    private String name;

    @Pattern(
            regexp = "^\\w{3,30}$",
            message = "{validation.user.username}"
    )
    @NotBlank(message = "{validation.user.username.not_blank}")
    private String username;

    @Email(message = "{validation.user.email.invalid}")
    @NotBlank(message = "{validation.user.email.not_blank}")
    @Size(min = 6, max = 320, message = "{validation.user.email.size}")
    private String email;

    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&-+=()])(?=\\S+$).{8,20}$",
            message = "{validation.user.password}"
    )
    @NotNull(message = "{validation.user.password.not_null}")
    private String password;

    private Gender gender;

    @Size(max = 255, message = "{validation.user.bio.size}")
    @JsonDeserialize(using = BasicHtmlSanitizationDeserializer.class)
    private String bio;

    @Past(message = "{validation.user.dob}")
    @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
    @NotNull(message = "{validation.user.dob.not_null}")
    private LocalDate dateOfBirth;
}
