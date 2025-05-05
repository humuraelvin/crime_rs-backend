package com.crime.reporting.crime_reporting_backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class MfaService {
    private final EmailService emailService;
    
    // Store verification codes with expiration times
    // Key: email, Value: [code, expirationTime]
    private final Map<String, Object[]> verificationCodes = new ConcurrentHashMap<>();
    
    // Code expiration time in minutes
    private static final int CODE_EXPIRATION_MINUTES = 10;
    
    /**
     * Generates a verification code and sends it to the user's email
     * @param email the user's email
     * @return the generated verification code
     */
    public String generateAndSendEmailVerificationCode(String email) {
        String code = generateVerificationCode();
        LocalDateTime expirationTime = LocalDateTime.now().plusMinutes(CODE_EXPIRATION_MINUTES);
        
        // Store the code with expiration time
        verificationCodes.put(email, new Object[]{code, expirationTime});
        
        // Send the code via email
        sendVerificationEmail(email, code);
        
        log.info("Generated verification code for {}: {}", email, code);
        return code;
    }
    
    /**
     * Validates an email verification code
     * @param email the user's email
     * @param code the verification code to validate
     * @return true if the code is valid, false otherwise
     */
    public boolean validateVerificationCode(String email, String code) {
        Object[] storedData = verificationCodes.get(email);
        
        if (storedData == null) {
            log.warn("No verification code found for {}", email);
            return false;
        }
        
        String storedCode = (String) storedData[0];
        LocalDateTime expirationTime = (LocalDateTime) storedData[1];
        
        // Check if the code has expired
        if (LocalDateTime.now().isAfter(expirationTime)) {
            log.warn("Verification code for {} has expired", email);
            verificationCodes.remove(email);
            return false;
        }
        
        // Validate the code
        boolean isValid = storedCode.equals(code);
        
        // If valid, remove the code to prevent reuse
        if (isValid) {
            log.info("Valid verification code for {}", email);
            verificationCodes.remove(email);
        } else {
            log.warn("Invalid verification code attempt for {}", email);
        }
        
        return isValid;
    }
    
    /**
     * Generate a random 6-digit verification code
     * @return a 6-digit numeric code
     */
    private String generateVerificationCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000); // 6-digit code
        return String.valueOf(code);
    }
    
    /**
     * Send verification code to user's email
     * @param email the user's email
     * @param code the verification code
     */
    private void sendVerificationEmail(String email, String code) {
        String subject = "Police & Crime Management System - Login Verification Code";
        String message = "Hello,\n\n" +
                "Your verification code for Police & Crime Management System login is: " + code + "\n\n" +
                "This code will expire in " + CODE_EXPIRATION_MINUTES + " minutes.\n\n" +
                "If you did not request this code, please ignore this email or contact support if you believe this is unauthorized activity.\n\n" +
                "Thank you,\n" +
                "Police & Crime Management System Team";
        
        emailService.sendEmail(email, subject, message);
    }
    
    /**
     * Clear expired codes (can be called periodically)
     */
    public void clearExpiredCodes() {
        LocalDateTime now = LocalDateTime.now();
        verificationCodes.entrySet().removeIf(entry -> {
            LocalDateTime expiration = (LocalDateTime) entry.getValue()[1];
            return now.isAfter(expiration);
        });
    }
} 