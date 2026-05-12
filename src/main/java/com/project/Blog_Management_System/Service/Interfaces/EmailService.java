package com.project.Blog_Management_System.Service.Interfaces;

public interface EmailService {

    void sendEmail(String toEmail, String subject, String body);
}
