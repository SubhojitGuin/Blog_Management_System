package com.project.Blog_Management_System.Service.Interfaces;

import com.project.Blog_Management_System.Dto.EmailMessageDTO;

public interface EmailTemplateService {

    EmailMessageDTO buildNewPostByAuthor(String followerName, String authorName, String postTitle, String postUrl) throws Exception;

    EmailMessageDTO buildPostPublished(String authorName, String postTitle, String postUrl) throws Exception;

    EmailMessageDTO buildPostCommented(String authorName, String commenterName, String postTitle, String postUrl, String commentSnippet) throws Exception;

    EmailMessageDTO buildCommentReplied(String parentCommenterName, String childCommenterName, String authorName, String postTitle, String originalComment, String replySnippet, String postUrl) throws Exception;

    EmailMessageDTO buildPostLiked(String authorName, String likerName, String postTitle, String postUrl) throws Exception;

    EmailMessageDTO buildNewFollower(String followeeName, String followerName, String followerProfileUrl) throws Exception;

}
