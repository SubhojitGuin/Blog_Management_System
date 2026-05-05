package com.project.Blog_Management_System.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EmailUpdateDTO {
    @Email
    @NotBlank
    @Size(min = 6, max = 320)
    private String email;
}
