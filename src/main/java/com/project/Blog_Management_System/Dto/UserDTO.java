package com.project.Blog_Management_System.Dto;

import lombok.Data;

@Data
public class UserDTO {
    private String id;
    private String name;
    private String username;
    private String bio;
    private Integer noOfFollowers;
    private Integer noOfFollowings;
    private Integer noOfPosts;
    private Boolean active;
    private Boolean isCurrentUser;
}
