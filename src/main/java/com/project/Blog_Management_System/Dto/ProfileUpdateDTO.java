package com.project.Blog_Management_System.Dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.project.Blog_Management_System.Deserializers.BasicHtmlSanitizationDeserializer;
import com.project.Blog_Management_System.Deserializers.StringSanitizationDeserializer;
import com.project.Blog_Management_System.Enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Data;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.time.LocalDate;

@Data
public class ProfileUpdateDTO {
    @NotBlank
    @Size(min = 2, max = 255)
    @JsonDeserialize(using = StringSanitizationDeserializer.class)
    private String name;

    @Size(max = 255)
    @JsonDeserialize(using = BasicHtmlSanitizationDeserializer.class)
    private String bio;

    private Gender gender;

    @Past(message = "Date of birth must be in the past and in the format yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
    @NotNull
    private LocalDate dateOfBirth;
}
