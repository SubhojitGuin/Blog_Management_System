package com.project.Blog_Management_System.Service;

import com.project.Blog_Management_System.Dto.EmailMessageDTO;
import com.project.Blog_Management_System.Service.Interfaces.EmailTemplateService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailTemplateServiceImpl implements EmailTemplateService {

    @Value("${spring.application.name}")
    private String appName;

    @Override
    public EmailMessageDTO buildNewPostByAuthor(String followerName, String authorName, String postTitle, String postUrl) {

        String subject = "New post published by %s".formatted(authorName);

        String body = """
                Hi %s,
 
                %s just published a new post: "%s"
 
                Read it here: %s
 
                You are receiving this email because you follow %s on %s.
 
                — The %s Team
                """.formatted(
                followerName,
                authorName,
                postTitle,
                postUrl,
                authorName,
                appName,
                appName);

        return EmailMessageDTO.builder()
                .subject(subject)
                .body(body)
                .build();
    }

    @Override
    public EmailMessageDTO buildPostPublished(String authorName, String postTitle, String postUrl) {

        String subject = "Scheduled Post: \"%s\" is published".formatted(postTitle);

        String body = """
                Hi %s,
 
                Your scheduled post "%s" has been automatically published and is now live for all readers.
 
                View it here: %s
 
                — The %s Team
                """.formatted(
                authorName,
                postTitle,
                postUrl,
                appName);

        return EmailMessageDTO.builder()
                .subject(subject)
                .body(body)
                .build();
    }

    @Override
    public EmailMessageDTO buildPostCommented(String authorName, String commenterName, String postTitle, String postUrl, String commentSnippet) {

        String subject = "%s commented on your post \"%s\"".formatted(commenterName, postTitle);

        String body = """
                Hi %s,
 
                %s commented on your post "%s":
 
                  "%s"
 
                View the comment: %s
 
                — The %s Team
                """.formatted(
                authorName,
                commenterName,
                postTitle,
                commentSnippet,
                postUrl,
                appName);

        return EmailMessageDTO.builder()
                .subject(subject)
                .body(body)
                .build();
    }

    @Override
    public EmailMessageDTO buildCommentReplied(String parentCommenterName, String childCommenterName, String postTitle, String originalComment, String replySnippet, String postUrl) {

        String subject = "%s replied to your comment on %s".formatted(childCommenterName, postTitle);

        String body = """
                Hi %s,
 
                %s replied to your comment on "%s":
 
                  Your comment:  "%s"
                  Their reply:   "%s"
 
                View the reply: %s
 
                — The %s Team
                """.formatted(
                parentCommenterName,
                childCommenterName,
                postTitle,
                originalComment,
                replySnippet,
                postUrl,
                appName);

        return EmailMessageDTO.builder()
                .subject(subject)
                .body(body)
                .build();
    }

    @Override
    public EmailMessageDTO buildPostLiked(String authorName, String likerName, String postTitle, String postUrl) {

        String  subject = "%s liked your post \"%s\"".formatted(likerName, postTitle);

        String body =  """
                Hi %s,
 
                %s liked your post "%s".
 
                View your post: %s
 
                — The %s Team
                """.formatted(
                authorName,
                likerName,
                postTitle,
                postUrl,
                appName);

        return EmailMessageDTO.builder()
                .subject(subject)
                .body(body)
                .build();
    }

    @Override
    public EmailMessageDTO buildNewFollower(String followeeName, String followerName, String followerProfileUrl) {

        String subject = "%s started following you".formatted(followerName);

        String body = """
                Hi %s,
 
                %s started following you on %s.
 
                View their profile: %s
 
                — The %s Team
                """.formatted(
                followeeName,
                followerName,
                appName,
                followerProfileUrl,
                appName);

        return EmailMessageDTO.builder()
                .subject(subject)
                .body(body)
                .build();
    }
}
