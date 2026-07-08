package com.project.Blog_Management_System.Utils;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TestPageResponse<T> {
    private List<T> content = new ArrayList<>();
    private Page page;

    @Data
    public static class Page {
        int number;
        int totalElements;
        int totalPages;
    }
}
