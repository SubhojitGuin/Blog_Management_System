package com.project.Blog_Management_System.Service.Interfaces;

import com.project.Blog_Management_System.Dto.EmailMessageDTO;

public interface EmailTemplateService {

    EmailMessageDTO buildNewPostByAuthor(String followerName, String authorName, String postTitle, String postUrl);

    EmailMessageDTO buildPostPublished(String authorName, String postTitle, String postUrl);

    EmailMessageDTO buildPostCommented(String authorName, String commenterName, String postTitle, String postUrl, String commentSnippet);

    EmailMessageDTO buildCommentReplied(String parentCommenterName, String childCommenterName, String postTitle, String originalComment, String replySnippet, String postUrl);

    EmailMessageDTO buildPostLiked(String authorName, String likerName, String postTitle, String postUrl);

    EmailMessageDTO buildNewFollower(String followeeName, String followerName, String followerProfileUrl);

}
