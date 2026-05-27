package com.project.Blog_Management_System.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailServiceImpl emailService;

    private final String toEmail = "receiver@example.com";
    private final String subject = "Test Subject";
    private final String body = "Test Body";

    @BeforeEach
    void setUp() {
        String fromEmail = "sender@blogsystem.com";
        ReflectionTestUtils.setField(emailService, "fromEmail", fromEmail);
    }


    @Nested
    @DisplayName("sendEmail(String, String, String)")
    class SendEmail {

        @Test
        @DisplayName("successfully constructs and sends plain text email configuration parameters")
        void successfullyConstructsAndSendsEmail() throws Exception {
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            doNothing().when(mailSender).send(any(MimeMessage.class));

            assertDoesNotThrow(() -> emailService.sendEmail(toEmail, subject, body));

            verify(mailSender).createMimeMessage();
            verify(mailSender).send(mimeMessage);
            verifyNoMoreInteractions(mailSender);
        }

        @Test
        @DisplayName("propagates MessagingException directly when thrown during helper construction assembly")
        void propagatesMessagingExceptionWhenHelperConstructionFails() throws Exception {
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            doThrow(new MailSendException("Invalid internal composition pipeline")).when(mailSender).send(any(MimeMessage.class));

            assertThrows(MailException.class, () -> emailService.sendEmail(toEmail, subject, body));

            verify(mailSender).createMimeMessage();
            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("invokes message generation and delivery steps in correct structural execution order")
        void invokesMessageGenerationAndDeliveryInCorrectOrder() throws Exception {
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            doNothing().when(mailSender).send(any(MimeMessage.class));

            emailService.sendEmail(toEmail, subject, body);

            InOrder inOrder = inOrder(mailSender);
            inOrder.verify(mailSender).createMimeMessage();
            inOrder.verify(mailSender).send(mimeMessage);
        }
    }

    @Nested
    @DisplayName("recoverMailException(MailException, String, String, String)")
    class RecoverMailException {

        @Test
        @DisplayName("swallows MailException and executes recovery logging paths cleanly without exceptions")
        void swallowsMailExceptionAndExecutesRecoveryLoggingCleanly() {
            MailException mailException = new MailSendException("Target host dropped active connection");

            assertDoesNotThrow(() -> emailService.recoverMailException(mailException, toEmail, subject, body));

            verifyNoInteractions(mailSender);
        }
    }

    @Nested
    @DisplayName("recoverMessagingException(MessagingException, String, String, String)")
    class RecoverMessagingException {

        @Test
        @DisplayName("rethrows incoming parsing exception to caller hierarchy context for external management")
        void rethrowsIncomingParsingExceptionToCallerHierarchyContext() {
            MessagingException messagingException = new MessagingException("Syntax formatting validation failure");

            MessagingException thrown = assertThrows(MessagingException.class, () ->
                emailService.recoverMessagingException(messagingException, toEmail, subject, body)
            );

            assertEquals("Syntax formatting validation failure", thrown.getMessage());
            verifyNoInteractions(mailSender);
        }
    }
}