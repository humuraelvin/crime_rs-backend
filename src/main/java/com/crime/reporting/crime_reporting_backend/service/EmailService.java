package com.crime.reporting.crime_reporting_backend.service;

public interface EmailService {
    void sendEmail(String to, String subject, String text);
    void sendPasswordResetCode(String to, String code);
} 