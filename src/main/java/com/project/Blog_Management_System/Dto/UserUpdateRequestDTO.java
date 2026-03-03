package com.project.Blog_Management_System.Dto;

import com.project.Blog_Management_System.Enums.Gender;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserUpdateRequestDTO {
    private String name;
    private String email;
    private String bio;
    private Gender gender;
    private LocalDate dateOfBirth;
}
