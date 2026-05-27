package com.project.Blog_Management_System.Service;

import com.project.Blog_Management_System.Dto.EmailMessageDTO;
import com.project.Blog_Management_System.TemplateModels.*;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailTemplateServiceImplTest {

    @Mock
    private Configuration freemarkerConfig;

    @Mock
    private Template template;

    @InjectMocks
    private EmailTemplateServiceImpl emailTemplateService;

    private MockedStatic<FreeMarkerTemplateUtils> mockedTemplateUtils;

    private final String mockHtmlBody = "<html>Mocked HTML Template Content</html>";

    @BeforeEach
    void setUp() {
        String appName = "Blog-System-Test";
        ReflectionTestUtils.setField(emailTemplateService, "appName", appName);

        mockedTemplateUtils = mockStatic(FreeMarkerTemplateUtils.class);
    }

    @AfterEach
    void tearDown() {
        mockedTemplateUtils.close();
    }

    @Nested
    @DisplayName("buildNewPostByAuthor(String, String, String, String)")
    class BuildNewPostByAuthor {

        @Test
        @DisplayName("returns mapped EmailMessageDTO with structured subject and processed content body")
        void returnsMessageDtoOnSuccessfulProcessing() throws Exception {
            String templateName = "new-post-template.ftlh";
            String expectedSubject = "New post published by Jane Doe";

            when(freemarkerConfig.getTemplate(templateName)).thenReturn(template);
            mockedTemplateUtils.when(() -> FreeMarkerTemplateUtils.processTemplateIntoString(eq(template), any(NewPostByAuthorModel.class)))
                    .thenReturn(mockHtmlBody);

            EmailMessageDTO result = emailTemplateService.buildNewPostByAuthor("John Doe", "Jane Doe", "Java 21 Features", "http://blog.com");

            assertNotNull(result);
            assertEquals(expectedSubject, result.getSubject());
            assertEquals(mockHtmlBody, result.getBody());

            verify(freemarkerConfig).getTemplate(templateName);
            mockedTemplateUtils.verify(() -> FreeMarkerTemplateUtils.processTemplateIntoString(eq(template), any(NewPostByAuthorModel.class)));
        }

        @Test
        @DisplayName("propagates generic Exception up when freemarker configuration cannot load file definition")
        void propagatesExceptionWhenConfigurationLoadingFails() throws Exception {
            when(freemarkerConfig.getTemplate("new-post-template.ftlh")).thenThrow(new IOException("Template not found"));

            assertThrows(IOException.class, () ->
                    emailTemplateService.buildNewPostByAuthor("John Doe", "Jane Doe", "Java 21 Features", "http://blog.com")
            );

            verify(freemarkerConfig).getTemplate("new-post-template.ftlh");
            mockedTemplateUtils.verifyNoInteractions();
        }

        @Test
        @DisplayName("invokes template retrieval and body engine processing operations in sequential order")
        void invokesRetrievalAndProcessingInCorrectSequence() throws Exception {
            when(freemarkerConfig.getTemplate("new-post-template.ftlh")).thenReturn(template);
            mockedTemplateUtils.when(() -> FreeMarkerTemplateUtils.processTemplateIntoString(eq(template), any(NewPostByAuthorModel.class)))
                    .thenReturn(mockHtmlBody);

            emailTemplateService.buildNewPostByAuthor("John Doe", "Jane Doe", "Java 21 Features", "http://blog.com");

            InOrder inOrder = inOrder(freemarkerConfig);
            inOrder.verify(freemarkerConfig).getTemplate("new-post-template.ftlh");
            mockedTemplateUtils.verify(() -> FreeMarkerTemplateUtils.processTemplateIntoString(eq(template), any(NewPostByAuthorModel.class)));
        }
    }

    @Nested
    @DisplayName("buildPostPublished(String, String, String)")
    class BuildPostPublished {

        @Test
        @DisplayName("returns mapped EmailMessageDTO with formatted post title inside notification header")
        void returnsMessageDtoOnSuccessfulProcessing() throws Exception {
            String templateName = "post-published-template.ftlh";
            String expectedSubject = "Scheduled Post: \"Spring Framework 6\" is published";

            when(freemarkerConfig.getTemplate(templateName)).thenReturn(template);
            mockedTemplateUtils.when(() -> FreeMarkerTemplateUtils.processTemplateIntoString(eq(template), any(PostPublishedModel.class)))
                    .thenReturn(mockHtmlBody);

            EmailMessageDTO result = emailTemplateService.buildPostPublished("Jane Doe", "Spring Framework 6", "http://blog.com");

            assertNotNull(result);
            assertEquals(expectedSubject, result.getSubject());
            assertEquals(mockHtmlBody, result.getBody());

            verify(freemarkerConfig).getTemplate(templateName);
            mockedTemplateUtils.verify(() -> FreeMarkerTemplateUtils.processTemplateIntoString(eq(template), any(PostPublishedModel.class)));
        }
    }

    @Nested
    @DisplayName("buildPostCommented(String, String, String, String, String)")
    class BuildPostCommented {

        @Test
        @DisplayName("returns mapped EmailMessageDTO linking commentator information and original post title")
        void returnsMessageDtoOnSuccessfulProcessing() throws Exception {
            String templateName = "post-comment-template.ftlh";
            String expectedSubject = "Alice commented on your post \"Docker Tips\"";

            when(freemarkerConfig.getTemplate(templateName)).thenReturn(template);
            mockedTemplateUtils.when(() -> FreeMarkerTemplateUtils.processTemplateIntoString(eq(template), any(PostCommentedModel.class)))
                    .thenReturn(mockHtmlBody);

            EmailMessageDTO result = emailTemplateService.buildPostCommented("Jane Doe", "Alice", "Docker Tips", "http://blog.com", "Great read!");

            assertNotNull(result);
            assertEquals(expectedSubject, result.getSubject());
            assertEquals(mockHtmlBody, result.getBody());

            verify(freemarkerConfig).getTemplate(templateName);
            mockedTemplateUtils.verify(() -> FreeMarkerTemplateUtils.processTemplateIntoString(eq(template), any(PostCommentedModel.class)));
        }
    }

    @Nested
    @DisplayName("buildCommentReplied(String, String, String, String, String, String, String)")
    class BuildCommentReplied {

        @Test
        @DisplayName("returns mapped EmailMessageDTO matching specific parent child conversation dynamics")
        void returnsMessageDtoOnSuccessfulProcessing() throws Exception {
            String templateName = "comment-reply-template.ftlh";
            String expectedSubject = "Bob replied to your comment on Docker Tips";

            when(freemarkerConfig.getTemplate(templateName)).thenReturn(template);
            mockedTemplateUtils.when(() -> FreeMarkerTemplateUtils.processTemplateIntoString(eq(template), any(CommentRepliedModel.class)))
                    .thenReturn(mockHtmlBody);

            EmailMessageDTO result = emailTemplateService.buildCommentReplied("Alice", "Bob", "Jane Doe", "Docker Tips", "Great read!", "I agree!", "http://blog.com");

            assertNotNull(result);
            assertEquals(expectedSubject, result.getSubject());
            assertEquals(mockHtmlBody, result.getBody());

            verify(freemarkerConfig).getTemplate(templateName);
            mockedTemplateUtils.verify(() -> FreeMarkerTemplateUtils.processTemplateIntoString(eq(template), any(CommentRepliedModel.class)));
        }
    }

    @Nested
    @DisplayName("buildPostLiked(String, String, String, String)")
    class BuildPostLiked {

        @Test
        @DisplayName("returns mapped EmailMessageDTO verifying target interaction triggers match incoming post names")
        void returnsMessageDtoOnSuccessfulProcessing() throws Exception {
            String templateName = "post-like-template.ftlh";
            String expectedSubject = "Alice liked your post \"Mockito Tutorial\"";

            when(freemarkerConfig.getTemplate(templateName)).thenReturn(template);
            mockedTemplateUtils.when(() -> FreeMarkerTemplateUtils.processTemplateIntoString(eq(template), any(PostLikedModel.class)))
                    .thenReturn(mockHtmlBody);

            EmailMessageDTO result = emailTemplateService.buildPostLiked("Jane Doe", "Alice", "Mockito Tutorial", "http://blog.com");

            assertNotNull(result);
            assertEquals(expectedSubject, result.getSubject());
            assertEquals(mockHtmlBody, result.getBody());

            verify(freemarkerConfig).getTemplate(templateName);
            mockedTemplateUtils.verify(() -> FreeMarkerTemplateUtils.processTemplateIntoString(eq(template), any(PostLikedModel.class)));
        }
    }

    @Nested
    @DisplayName("buildNewFollower(String, String, String)")
    class BuildNewFollower {
        @Test
        @DisplayName("returns mapped EmailMessageDTO capturing simple user connection follower metrics")
        void returnsMessageDtoOnSuccessfulProcessing() throws Exception {
            String templateName = "new-follower-template.ftlh";
            String expectedSubject = "Charlie started following you";

            when(freemarkerConfig.getTemplate(templateName)).thenReturn(template);
            mockedTemplateUtils.when(() -> FreeMarkerTemplateUtils.processTemplateIntoString(eq(template), any(NewFollowerModel.class))).thenReturn(mockHtmlBody);

            EmailMessageDTO result = emailTemplateService.buildNewFollower("Jane Doe", "Charlie", "blog.com");

            assertNotNull(result);
            assertEquals(expectedSubject, result.getSubject());
            assertEquals(mockHtmlBody, result.getBody());

            verify(freemarkerConfig).getTemplate(templateName);
            mockedTemplateUtils.verify(() -> FreeMarkerTemplateUtils.processTemplateIntoString(eq(template), any(NewFollowerModel.class)));
        }
    }
}
