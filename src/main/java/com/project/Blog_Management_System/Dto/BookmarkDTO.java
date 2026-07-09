package com.project.Blog_Management_System.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BookmarkDTO {
    @NotNull(message = "{validation.bookmark.bookmark.not_null}")
    private Boolean bookmark;
}
