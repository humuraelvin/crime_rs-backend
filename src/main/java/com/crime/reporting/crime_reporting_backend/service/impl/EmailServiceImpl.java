package com.crime.reporting.crime_reporting_backend.service.impl;

import com.crime.reporting.crime_reporting_backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender emailSender;

    @Override
    public void sendEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            emailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Override
    public void sendPasswordResetCode(String to, String code) {
        String subject = "Crime Reporting System - Password Reset Code";
        String message = "Your password reset code is: " + code + "\n\n" +
                "This code will expire in 15 minutes.\n\n" +
                "If you did not request this code, please ignore this email.";
        
        sendEmail(to, subject, message);
    }
} 