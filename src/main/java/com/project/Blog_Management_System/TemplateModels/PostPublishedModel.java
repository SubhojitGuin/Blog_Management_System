package com.project.Blog_Management_System.TemplateModels;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PostPublishedModel {
    String authorName;
    String postTitle;
    String postUrl;
    String appName;
}
