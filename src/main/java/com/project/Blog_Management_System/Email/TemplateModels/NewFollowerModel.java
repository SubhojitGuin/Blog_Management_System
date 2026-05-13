package com.project.Blog_Management_System.Email.TemplateModels;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NewFollowerModel {
    String followeeName;
    String followerName;
    String profileUrl;
    String appName;
}
