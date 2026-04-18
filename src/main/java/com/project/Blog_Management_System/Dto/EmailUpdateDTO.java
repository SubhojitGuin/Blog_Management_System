package com.project.Blog_Management_System.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmailUpdateDTO {
    @Email
    @NotBlank
    private String email;
}
