package com.project.Blog_Management_System.TemplateModels;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PostCommentedModel {
    String authorName;
    String commenterName;
    String postTitle;
    String commentSnippet;
    String postUrl;
    String appName;
}
