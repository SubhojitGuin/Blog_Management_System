package com.project.Blog_Management_System.Email.TemplateModels;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NewPostByAuthorModel {
    String followerName;
    String authorName;
    String postTitle;
    String postUrl;
    String appName;
}
