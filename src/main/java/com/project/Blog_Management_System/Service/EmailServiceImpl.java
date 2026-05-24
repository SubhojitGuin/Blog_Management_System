package com.project.Blog_Management_System.Service;

import com.project.Blog_Management_System.Service.Interfaces.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Override
    @Retryable(
            retryFor = {MailException.class, MessagingException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2.0)
    )
    public void sendEmail(String toEmail, String subject, String body) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false);

        helper.setFrom(fromEmail);
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(body, false);

        mailSender.send(message);
        log.info("Email sent to: {}", toEmail);
    }

    /**
     * Recovery method for MailException. This method will be called after all retry attempts have been exhausted. It logs the failure and the exception message for further analysis.
     */
    @Recover
    private void recoverMailException(MailException ex, String toEmail, String subject, String body) {
        log.error("Could not send email to {} due to: {}", toEmail, ex.getMessage());
    }

    /**
     * Recovery method for MessagingException. Since MessagingException is not retriable, this method will be called immediately when such an exception is thrown. It rethrows the exception to be handled by the caller.
     */
    @Recover
    private void recoverMessagingException(MessagingException ex, String toEmail, String subject, String body) throws MessagingException {
        throw ex;
    }
}