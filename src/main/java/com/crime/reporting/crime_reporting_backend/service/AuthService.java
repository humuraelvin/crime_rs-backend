package com.crime.reporting.crime_reporting_backend.service;

import com.crime.reporting.crime_reporting_backend.dto.UserResponse;
import com.crime.reporting.crime_reporting_backend.dto.request.*;
import com.crime.reporting.crime_reporting_backend.dto.response.AuthResponse;
import com.crime.reporting.crime_reporting_backend.dto.response.TwoFactorAuthSetupResponse;
import com.crime.reporting.crime_reporting_backend.entity.Role;
import com.crime.reporting.crime_reporting_backend.entity.User;
import com.crime.reporting.crime_reporting_backend.repository.UserRepository;
import com.crime.reporting.crime_reporting_backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final MfaService mfaService;
    
    // Replace Redis with in-memory maps
    private final Map<String, String> tokenBlacklist = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Transactional
    public AuthResponse register(UserRegistrationRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        // Create user entity
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole() != null ? request.getRole() : Role.CITIZEN)
                .phoneNumber(request.getPhoneNumber())
                .address(request.getAddress())
                .mfaEnabled(request.isEnableMfa())
                .build();

        // Save the user
        User savedUser = userRepository.save(user);

        // For MFA-enabled users, no tokens yet - they need to verify
        if (request.isEnableMfa()) {
            // Generate and send verification code via email
            mfaService.generateAndSendEmailVerificationCode(savedUser.getEmail());
            
            return AuthResponse.builder()
                    .userId(savedUser.getId())
                    .firstName(savedUser.getFirstName())
                    .lastName(savedUser.getLastName())
                    .email(savedUser.getEmail())
                    .role(savedUser.getRole())
                    .mfaEnabled(true)
                    .mfaRequired(true)
                    .build();
        }

        // Generate tokens for non-MFA users
        String accessToken = jwtService.generateToken(createClaims(savedUser), savedUser);
        String refreshToken = jwtService.generateRefreshToken(savedUser);

        // Return response with tokens
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(savedUser.getId())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .mfaEnabled(false)
                .mfaRequired(false)
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        // Authenticate the user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Set the authentication in the SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Get the user from the repository
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if MFA is enabled and no MFA code was provided
        if (user.isMfaEnabled() && (request.getMfaCode() == null || request.getMfaCode().isEmpty())) {
            // Generate and send verification code via email
            mfaService.generateAndSendEmailVerificationCode(user.getEmail());
            
            return AuthResponse.builder()
                    .userId(user.getId())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .email(user.getEmail())
                    .role(user.getRole())
                    .mfaEnabled(true)
                    .mfaRequired(true)
                    .build();
        }

        // If MFA is enabled and code was provided, verify it
        if (user.isMfaEnabled() && request.getMfaCode() != null) {
            if (!mfaService.validateVerificationCode(request.getEmail(), request.getMfaCode())) {
                throw new RuntimeException("Invalid verification code");
            }
        }

        // Generate tokens
        String accessToken = jwtService.generateToken(createClaims(user), user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // Return response with tokens
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole())
                .mfaEnabled(user.isMfaEnabled())
                .mfaRequired(false)
                .build();
    }

    public AuthResponse refreshToken(String refreshToken) {
        // Extract email from token
        String userEmail = jwtService.extractUsername(refreshToken);
        
        // Get the user from the repository
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Validate the refresh token
        if (!jwtService.isTokenValid(refreshToken, user)) {
            throw new RuntimeException("Invalid refresh token");
        }
        
        // Generate new tokens
        String accessToken = jwtService.generateToken(createClaims(user), user);
        String newRefreshToken = jwtService.generateRefreshToken(user);
        
        // Return response with tokens
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshToken)
                .userId(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole())
                .mfaEnabled(user.isMfaEnabled())
                .mfaRequired(false)
                .build();
    }

    public AuthResponse verifyTwoFactorAuthentication(String email, String mfaCode) {
        // Get the user from the repository
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Verify the MFA code
        if (!mfaService.validateVerificationCode(email, mfaCode)) {
            throw new RuntimeException("Invalid verification code");
        }
        
        // Generate tokens
        String accessToken = jwtService.generateToken(createClaims(user), user);
        String refreshToken = jwtService.generateRefreshToken(user);
        
        // Return response with tokens
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole())
                .mfaEnabled(true)
                .mfaRequired(false)
                .build();
    }

    public void revokeToken(String token) {
        tokenBlacklist.put(token, "revoked");
    }

    public void logout(String token) {
        revokeToken(token);
    }

    public TwoFactorAuthSetupResponse generateMfaSecret(String email) {
        // Get the user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Generate and send verification code via email
        String verificationCode = mfaService.generateAndSendEmailVerificationCode(email);
        
        // Return response with message (no QR code or secret key)
        return TwoFactorAuthSetupResponse.builder()
                .message("A verification code has been sent to your email. Please check your inbox.")
                .build();
    }

    public void enableMfa(String email, String mfaCode) {
        // Get the user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Verify the MFA code
        if (!mfaService.validateVerificationCode(email, mfaCode)) {
            throw new RuntimeException("Invalid verification code");
        }
        
        // Update user to enable MFA
        user.setMfaEnabled(true);
        userRepository.save(user);
    }

    public void disableMfa(String email, String password) {
        // Get the user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Check credentials
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }
        
        // Update user to disable MFA
        user.setMfaEnabled(false);
        userRepository.save(user);
    }

    private Map<String, Object> createClaims(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole());
        return claims;
    }

    /**
     * Changes the user's password
     * @param currentPassword the user's current password
     * @param newPassword the new password to set
     */
    @Transactional
    public void changePassword(String currentPassword, String newPassword) {
        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        
        // Get the user from the repository
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }
        
        // Update to new password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        log.info("Password changed successfully for user: {}", email);
    }

    /**
     * Get the user profile for the currently authenticated user
     * @param email the email of the user
     * @return the user profile data
     */
    @Transactional(readOnly = true)
    public UserResponse getUserProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .role(user.getRole())
                .emailNotifications(user.isEmailNotifications())
                .smsNotifications(user.isSmsNotifications())
                .mfaEnabled(user.isMfaEnabled())
                .build();
    }
    
    /**
     * Update the user profile for the currently authenticated user
     * @param email the email of the user
     * @param request the update request containing the new profile data
     * @return the updated user profile
     */
    @Transactional
    public UserResponse updateUserProfile(String email, UpdateUserProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Update user fields if they are provided in the request
        if (request.getFirstName() != null && !request.getFirstName().isEmpty()) {
            user.setFirstName(request.getFirstName());
        }
        
        if (request.getLastName() != null && !request.getLastName().isEmpty()) {
            user.setLastName(request.getLastName());
        }
        
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        
        // Check if notification preferences were updated
        if (request.getEmailNotifications() != null) {
            user.setEmailNotifications(request.getEmailNotifications());
        }
        
        if (request.getSmsNotifications() != null) {
            user.setSmsNotifications(request.getSmsNotifications());
        }
        
        User updatedUser = userRepository.save(user);
        log.info("Profile updated for user: {}", email);
        
        return UserResponse.builder()
                .id(updatedUser.getId())
                .firstName(updatedUser.getFirstName())
                .lastName(updatedUser.getLastName())
                .email(updatedUser.getEmail())
                .phoneNumber(updatedUser.getPhoneNumber())
                .address(updatedUser.getAddress())
                .role(updatedUser.getRole())
                .emailNotifications(updatedUser.isEmailNotifications())
                .smsNotifications(updatedUser.isSmsNotifications())
                .mfaEnabled(updatedUser.isMfaEnabled())
                .build();
    }
} 