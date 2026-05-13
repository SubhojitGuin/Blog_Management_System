package com.project.Blog_Management_System.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EmailUpdateDTO {
    @Email(message = "{validation.user.email.invalid}")
    @NotBlank(message = "{validation.user.email.not_blank}")
    @Size(min = 6, max = 320, message = "{validation.user.email.size}")
    private String email;
}
