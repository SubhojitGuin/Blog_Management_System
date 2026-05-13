package com.project.Blog_Management_System.Email.TemplateModels;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CommentRepliedModel {
    String parentCommenterName;
    String childCommenterName;
    String authorName;
    String postTitle;
    String originalComment;
    String replySnippet;
    String postUrl;
    String appName;
}
