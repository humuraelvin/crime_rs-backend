package com.crime.reporting.crime_reporting_backend.controller;

import com.crime.reporting.crime_reporting_backend.dto.request.ForgotPasswordRequest;
import com.crime.reporting.crime_reporting_backend.dto.request.ResetPasswordRequest;
import com.crime.reporting.crime_reporting_backend.dto.request.VerifyResetCodeRequest;
import com.crime.reporting.crime_reporting_backend.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/password")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;
    
    @PostMapping("/forgot")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        try {
            passwordResetService.generateResetCode(request);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Password reset code sent to your email");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PostMapping("/verify-code")
    public ResponseEntity<Map<String, Boolean>> verifyResetCode(@RequestBody VerifyResetCodeRequest request) {
        boolean isValid = passwordResetService.verifyResetCode(request);
        Map<String, Boolean> response = new HashMap<>();
        response.put("valid", isValid);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            passwordResetService.resetPassword(request);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Password has been reset successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
} 