package com.crime.reporting.crime_reporting_backend.service;

import com.crime.reporting.crime_reporting_backend.dto.request.ForgotPasswordRequest;
import com.crime.reporting.crime_reporting_backend.dto.request.ResetPasswordRequest;
import com.crime.reporting.crime_reporting_backend.dto.request.VerifyResetCodeRequest;
import com.crime.reporting.crime_reporting_backend.entity.User;
import com.crime.reporting.crime_reporting_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    
    // Store reset codes with expiration timestamp - in a real app, use a database or Redis
    private final Map<String, ResetCodeInfo> resetCodes = new ConcurrentHashMap<>();
    
    // Code expiration time in milliseconds (15 minutes)
    private static final long RESET_CODE_EXPIRATION = 15 * 60 * 1000;
    
    public void generateResetCode(ForgotPasswordRequest request) {
        String email = request.getEmail();
        
        // Check if user exists
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        
        // Generate a 6-digit code
        String resetCode = generateSixDigitCode();
        
        // Store the code with expiration time
        resetCodes.put(email, new ResetCodeInfo(resetCode, Instant.now().toEpochMilli() + RESET_CODE_EXPIRATION));
        
        // Send the code via email
        emailService.sendPasswordResetCode(email, resetCode);
        
        log.info("Password reset code generated for user: {}", email);
    }
    
    public boolean verifyResetCode(VerifyResetCodeRequest request) {
        String email = request.getEmail();
        String resetCode = request.getResetCode();
        
        // Check if code exists for this email
        ResetCodeInfo codeInfo = resetCodes.get(email);
        if (codeInfo == null) {
            log.warn("No reset code found for email: {}", email);
            return false;
        }
        
        // Check if code has expired
        if (codeInfo.expirationTime < Instant.now().toEpochMilli()) {
            log.warn("Reset code expired for email: {}", email);
            resetCodes.remove(email);
            return false;
        }
        
        // Verify the code
        if (!codeInfo.code.equals(resetCode)) {
            log.warn("Invalid reset code for email: {}", email);
            return false;
        }
        
        return true;
    }
    
    public void resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail();
        String resetCode = request.getResetCode();
        String newPassword = request.getNewPassword();
        
        // Verify the reset code first
        if (!verifyResetCode(new VerifyResetCodeRequest(email, resetCode))) {
            throw new RuntimeException("Invalid or expired reset code");
        }
        
        // Fetch the user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        
        // Update the password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        // Remove the used code
        resetCodes.remove(email);
        
        log.info("Password reset successful for user: {}", email);
    }
    
    private String generateSixDigitCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000); // Generates a number between 100000 and 999999
        return String.valueOf(code);
    }
    
    // Inner class to store reset code information
    private static class ResetCodeInfo {
        private final String code;
        private final long expirationTime;
        
        public ResetCodeInfo(String code, long expirationTime) {
            this.code = code;
            this.expirationTime = expirationTime;
        }
    }
} 