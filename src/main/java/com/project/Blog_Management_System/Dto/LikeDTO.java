package com.project.Blog_Management_System.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LikeDTO {
    @NotNull(message = "{validation.like.like.not_null}")
    private Boolean like;
}
