package com.project.Blog_Management_System.Dto;

import lombok.Data;

@Data
public class LoginRequestDTO {
    private String emailOrUsername;
    private String password;
}
