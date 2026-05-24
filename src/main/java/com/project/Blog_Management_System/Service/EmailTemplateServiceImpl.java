package com.project.Blog_Management_System.Service;

import com.project.Blog_Management_System.Dto.EmailMessageDTO;
import com.project.Blog_Management_System.Service.Interfaces.EmailTemplateService;
import com.project.Blog_Management_System.TemplateModels.*;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

@Service
@RequiredArgsConstructor
public class EmailTemplateServiceImpl implements EmailTemplateService {

    @Value("${spring.application.name}")
    private String appName;

    private final Configuration freemarkerConfig;

    @Override
    public EmailMessageDTO buildNewPostByAuthor(String followerName, String authorName, String postTitle, String postUrl) throws Exception {

        String subject = "New post published by %s".formatted(authorName);

        Template t = freemarkerConfig.getTemplate("new-post-template.ftlh");
        String body = FreeMarkerTemplateUtils.processTemplateIntoString(t, new NewPostByAuthorModel(
                followerName,
                authorName,
                postTitle,
                postUrl,
                appName
        ));

        return EmailMessageDTO.builder()
                .subject(subject)
                .body(body)
                .build();
    }

    @Override
    public EmailMessageDTO buildPostPublished(String authorName, String postTitle, String postUrl) throws Exception {

        String subject = "Scheduled Post: \"%s\" is published".formatted(postTitle);

        Template t = freemarkerConfig.getTemplate("post-published-template.ftlh");
        String body = FreeMarkerTemplateUtils.processTemplateIntoString(t, new PostPublishedModel(
                authorName,
                postTitle,
                postUrl,
                appName
        ));

        return EmailMessageDTO.builder()
                .subject(subject)
                .body(body)
                .build();
    }

    @Override
    public EmailMessageDTO buildPostCommented(String authorName, String commenterName, String postTitle, String postUrl, String commentSnippet) throws Exception {

        String subject = "%s commented on your post \"%s\"".formatted(commenterName, postTitle);

        Template t = freemarkerConfig.getTemplate("post-comment-template.ftlh");
        String body = FreeMarkerTemplateUtils.processTemplateIntoString(t, new PostCommentedModel(
                authorName,
                commenterName,
                postTitle,
                commentSnippet,
                postUrl,
                appName
        ));

        return EmailMessageDTO.builder()
                .subject(subject)
                .body(body)
                .build();
    }

    @Override
    public EmailMessageDTO buildCommentReplied(String parentCommenterName, String childCommenterName, String authorName, String postTitle, String originalComment, String replySnippet, String postUrl) throws Exception {

        String subject = "%s replied to your comment on %s".formatted(childCommenterName, postTitle);

        Template t = freemarkerConfig.getTemplate("comment-reply-template.ftlh");
        String body = FreeMarkerTemplateUtils.processTemplateIntoString(t, new CommentRepliedModel(
                parentCommenterName,
                childCommenterName,
                authorName,
                postTitle,
                originalComment,
                replySnippet,
                postUrl,
                appName
        ));

        return EmailMessageDTO.builder()
                .subject(subject)
                .body(body)
                .build();
    }

    @Override
    public EmailMessageDTO buildPostLiked(String authorName, String likerName, String postTitle, String postUrl) throws Exception {

        String  subject = "%s liked your post \"%s\"".formatted(likerName, postTitle);

        Template t = freemarkerConfig.getTemplate("post-like-template.ftlh");
        String body = FreeMarkerTemplateUtils.processTemplateIntoString(t, new PostLikedModel(
                authorName,
                likerName,
                postTitle,
                postUrl,
                appName
        ));

        return EmailMessageDTO.builder()
                .subject(subject)
                .body(body)
                .build();
    }

    @Override
    public EmailMessageDTO buildNewFollower(String followeeName, String followerName, String followerProfileUrl) throws Exception {

        String subject = "%s started following you".formatted(followerName);

        Template t = freemarkerConfig.getTemplate("new-follower-template.ftlh");
        String body = FreeMarkerTemplateUtils.processTemplateIntoString(t, new NewFollowerModel(
                followeeName,
                followerName,
                followerProfileUrl,
                appName
        ));

        return EmailMessageDTO.builder()
                .subject(subject)
                .body(body)
                .build();
    }
}
