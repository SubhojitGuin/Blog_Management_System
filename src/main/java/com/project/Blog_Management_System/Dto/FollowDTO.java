package com.project.Blog_Management_System.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FollowDTO {
    @NotNull(message = "{validation.follow.follow.not_null}")
    private Boolean follow;
}
