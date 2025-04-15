package com.crime.reporting.crime_reporting_backend.service.impl;

import com.crime.reporting.crime_reporting_backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender emailSender;
    
    @Value("${spring.mail.username}")
    private String emailFrom;

    @Override
    public void sendEmail(String to, String subject, String text) {
        try {
            MimeMessage mimeMessage = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, false); // false indicates this is not HTML
            helper.setFrom(emailFrom, "Crime Reporting System");
            
            emailSender.send(mimeMessage);
            log.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Override
    public void sendPasswordResetCode(String to, String code) {
        String subject = "Crime Reporting System - Password Reset Code";
        String message = "Hello,\n\n" +
                "You've requested to reset your password for your Crime Reporting System account.\n\n" +
                "Your password reset code is: " + code + "\n\n" +
                "This code will expire in 15 minutes.\n\n" +
                "If you did not request this code, please ignore this email or contact support if you believe this is unauthorized activity.\n\n" +
                "Thank you,\n" +
                "Crime Reporting System Team";
        
        sendEmail(to, subject, message);
    }
} 