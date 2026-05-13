package com.project.Blog_Management_System.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LikeDTO {
    @NotNull(message = "{validation.like.like.not_null}")
    private Boolean like;
}
