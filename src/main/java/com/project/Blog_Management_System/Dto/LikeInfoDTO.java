package com.project.Blog_Management_System.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LikeInfoDTO {
    private UserInfoDTO user;
    private UUID likeId;
}
