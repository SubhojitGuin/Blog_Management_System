package com.project.Blog_Management_System.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FollowDTO {
    @NotNull
    private Boolean follow;
}
