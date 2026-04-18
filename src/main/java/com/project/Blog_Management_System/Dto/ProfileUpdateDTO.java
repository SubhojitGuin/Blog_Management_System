package com.project.Blog_Management_System.Dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.project.Blog_Management_System.Enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ProfileUpdateDTO {
    @NotBlank
    private String name;

    private String bio;

    @NotBlank
    private Gender gender;

    @Past(message = "Date of birth must be in the past and in the format yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
    @NotNull
    private LocalDate dateOfBirth;
}
