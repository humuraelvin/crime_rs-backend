package com.crime.reporting.crime_reporting_backend.service;

import com.crime.reporting.crime_reporting_backend.dto.request.*;
import com.crime.reporting.crime_reporting_backend.dto.response.AuthResponse;
import com.crime.reporting.crime_reporting_backend.dto.response.TwoFactorAuthSetupResponse;
import com.crime.reporting.crime_reporting_backend.entity.Role;
import com.crime.reporting.crime_reporting_backend.entity.User;
import com.crime.reporting.crime_reporting_backend.repository.UserRepository;
import com.crime.reporting.crime_reporting_backend.security.JwtService;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final MfaService mfaService;
    
    // Replace Redis with in-memory maps
    private final Map<String, String> tokenBlacklist = new ConcurrentHashMap<>();
    private final Map<String, String> mfaSetupSecrets = new ConcurrentHashMap<>();
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

        // If MFA is enabled, generate a secret
        if (request.isEnableMfa()) {
            user.setMfaSecret(generateMfaSetupSecret(request.getEmail()));
            // Save the user
            User savedUser = userRepository.save(user);

            // Return response without tokens, requiring MFA verification
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

        // Save the user
        User savedUser = userRepository.save(user);

        // Generate tokens
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
            if (!verifyMfaSetup(request.getEmail(), request.getMfaCode())) {
                throw new RuntimeException("Invalid MFA code");
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
        if (!verifyMfaSetup(email, mfaCode)) {
            throw new RuntimeException("Invalid MFA code");
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
        String email = jwtService.extractUsername(token);
        long ttl = jwtService.getExpirationTime(token) - System.currentTimeMillis();
        
        if (ttl > 0) {
            // Add token to blacklist with expiration
            tokenBlacklist.put("BL_" + token, email);
            
            // Schedule removal from blacklist after token expiration
            scheduler.schedule(() -> tokenBlacklist.remove("BL_" + token), ttl, TimeUnit.MILLISECONDS);
        }
    }

    public void logout(String token) {
        revokeToken(token);
    }

    public TwoFactorAuthSetupResponse generateMfaSecret(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        String secret = mfaService.generateSecret();
        String qrCodeImageUri = mfaService.generateQrCodeImageUri(email, secret);
        
        // Store in memory temporarily until confirmed
        mfaSetupSecrets.put("MFA_SETUP_" + email, secret);
        
        // Schedule removal after 10 minutes
        scheduler.schedule(() -> mfaSetupSecrets.remove("MFA_SETUP_" + email), 10, TimeUnit.MINUTES);
        
        return TwoFactorAuthSetupResponse.builder()
                .secretKey(secret)
                .qrCodeImageUri(qrCodeImageUri)
                .build();
    }
    
    public void enableMfa(String email, String mfaCode) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        String secretKey = mfaSetupSecrets.get("MFA_SETUP_" + email);
        if (secretKey == null) {
            throw new RuntimeException("MFA setup expired. Please try again.");
        }
        
        if (!mfaService.validateTotp(secretKey, mfaCode)) {
            throw new RuntimeException("Invalid MFA code");
        }
        
        // Update user with MFA enabled and save the secret
        user.setMfaEnabled(true);
        user.setMfaSecret(secretKey);
        userRepository.save(user);
        
        // Clean up the temporary secret
        mfaSetupSecrets.remove("MFA_SETUP_" + email);
    }
    
    public void disableMfa(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Verify password before disabling MFA
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }
        
        // Update user with MFA disabled
        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        userRepository.save(user);
    }

    public String generateMfaSetupSecret(String email) {
        String secret = mfaService.generateSecret();
        
        // Store in memory with expiration (10 minutes)
        mfaSetupSecrets.put("MFA_SETUP_" + email, secret);
        
        // Schedule removal after 10 minutes
        scheduler.schedule(() -> mfaSetupSecrets.remove("MFA_SETUP_" + email), 10, TimeUnit.MINUTES);
        
        return secret;
    }

    public boolean verifyMfaSetup(String email, String code) {
        String cachedSecret = mfaSetupSecrets.get("MFA_SETUP_" + email);
        
        if (cachedSecret == null) {
            throw new IllegalStateException("MFA setup has expired. Please restart the setup process.");
        }
        
        boolean isValid = mfaService.validateTotp(cachedSecret, code);
        
        if (isValid) {
            // Remove from temporary cache
            mfaSetupSecrets.remove("MFA_SETUP_" + email);
            
            // Update user with the secret
            userService.enableMfa(email, cachedSecret);
        }
        
        return isValid;
    }

    private String generateSecret() {
        SecretGenerator secretGenerator = new DefaultSecretGenerator();
        return secretGenerator.generate();
    }

    private String generateQrCodeImageUri(String email, String secret) {
        QrData data = new QrData.Builder()
                .label(email)
                .secret(secret)
                .issuer("Crime Reporting System")
                .algorithm(dev.samstevens.totp.code.HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();

        QrGenerator generator = new ZxingPngQrGenerator();
        try {
            byte[] imageData = generator.generate(data);
            return getDataUriForImage(imageData, generator.getImageMimeType());
        } catch (dev.samstevens.totp.exceptions.QrGenerationException e) {
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    private boolean verifyCode(String code, String secret) {
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
        return verifier.isValidCode(secret, code);
    }

    private Map<String, Object> createClaims(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole());
        return claims;
    }
} 