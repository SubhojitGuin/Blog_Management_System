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
    @NotBlank(message = "{validation.user.name.not_blank}")
    @Size(min = 2, max = 255, message = "{validation.user.name.size}")
    @JsonDeserialize(using = StringSanitizationDeserializer.class)
    private String name;

    @Size(max = 255, message = "{validation.user.bio.size}")
    @JsonDeserialize(using = BasicHtmlSanitizationDeserializer.class)
    private String bio;

    private Gender gender;

    @Past(message = "{validation.user.dob}")
    @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
    @NotNull(message = "{validation.user.dob.not_null}")
    private LocalDate dateOfBirth;
}
