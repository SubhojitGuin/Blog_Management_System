package com.project.Blog_Management_System.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookmarkDTO {
    @NotNull(message = "{validation.bookmark.bookmark.not_null}")
    private Boolean bookmark;
}
