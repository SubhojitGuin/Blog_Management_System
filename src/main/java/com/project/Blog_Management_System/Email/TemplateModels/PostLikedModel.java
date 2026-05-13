package com.project.Blog_Management_System.Email.TemplateModels;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PostLikedModel {
    String authorName;
    String likerName;
    String postTitle;
    String postUrl;
    String appName;
}
