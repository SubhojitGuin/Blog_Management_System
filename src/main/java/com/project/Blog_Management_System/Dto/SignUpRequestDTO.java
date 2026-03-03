package com.project.Blog_Management_System.Dto;

import com.project.Blog_Management_System.Enums.Gender;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SignUpRequestDTO {
    private String name;
    private String username;
    private String email;
    private String password;
    private Gender gender;
    private String bio;
    private LocalDate dateOfBirth;
}
