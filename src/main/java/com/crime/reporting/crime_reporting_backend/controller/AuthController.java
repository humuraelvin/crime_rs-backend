package com.crime.reporting.crime_reporting_backend.controller;

import com.crime.reporting.crime_reporting_backend.dto.UserResponse;
import com.crime.reporting.crime_reporting_backend.dto.request.*;
import com.crime.reporting.crime_reporting_backend.dto.response.AuthResponse;
import com.crime.reporting.crime_reporting_backend.dto.response.TwoFactorAuthSetupResponse;
import com.crime.reporting.crime_reporting_backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody UserRegistrationRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request.getRefreshToken()));
    }

    @PostMapping("/verify-2fa")
    public ResponseEntity<AuthResponse> verifyTwoFactorAuthentication(
            @Valid @RequestBody TwoFactorAuthenticationRequest request) {
        return ResponseEntity.ok(authService.verifyTwoFactorAuthentication(
                request.getEmail(), request.getMfaCode()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String token) {
        authService.logout(token.substring(7));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/mfa/generate")
    public ResponseEntity<TwoFactorAuthSetupResponse> generateMfaSecret(@RequestParam String email) {
        return ResponseEntity.ok(authService.generateMfaSecret(email));
    }

    @PostMapping("/mfa/enable")
    public ResponseEntity<Void> enableMfa(@Valid @RequestBody EnableMfaRequest request) {
        authService.enableMfa(request.getEmail(), request.getMfaCode());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/mfa/disable")
    public ResponseEntity<Void> disableMfa(@Valid @RequestBody DisableMfaRequest request) {
        authService.disableMfa(request.getEmail(), request.getPassword());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/users/profile")
    public ResponseEntity<UserResponse> getUserProfile(Authentication authentication) {
        return ResponseEntity.ok(authService.getUserProfile(authentication.getName()));
    }

    @PutMapping("/users/profile")
    public ResponseEntity<UserResponse> updateUserProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        return ResponseEntity.ok(authService.updateUserProfile(authentication.getName(), request));
    }
} 