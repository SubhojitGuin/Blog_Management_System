package com.project.Blog_Management_System.Dto;

import lombok.Data;

import java.util.UUID;

@Data
public class UserDTO {
    private UUID id;
    private String name;
    private String username;
    private String bio;
    private Integer noOfFollowers = 0;
    private Integer noOfFollowings = 0;
    private Integer noOfPosts = 0;
    private Boolean isDeleted = false;
    private Boolean isCurrentUser = true;
}
