package com.project.Blog_Management_System.Utils;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TestSliceResponse<T> {
    private List<T> content = new ArrayList<>();
    private int numberOfElements;
    private boolean first;
    private boolean last;
    private boolean empty;
}
