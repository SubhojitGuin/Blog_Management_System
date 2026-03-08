package com.project.Blog_Management_System.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class UserInfoDTO {
    private UUID id;
    private String name;
    private String username;
    private Boolean active;
}
