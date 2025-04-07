package com.crime.reporting.crime_reporting_backend.service;

import com.crime.reporting.crime_reporting_backend.dto.AuthRequest;
import com.crime.reporting.crime_reporting_backend.dto.AuthResponse;
import com.crime.reporting.crime_reporting_backend.dto.UserRegistrationRequest;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RedisTemplate<String, String> redisTemplate;

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
            user.setMfaSecret(generateSecret());
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

    public AuthResponse login(AuthRequest request) {
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
            if (!verifyCode(request.getMfaCode(), user.getMfaSecret())) {
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
        if (!verifyCode(mfaCode, user.getMfaSecret())) {
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

    public void logout(String token) {
        // Add token to blacklist in Redis with expiration
        String email = jwtService.extractUsername(token);
        Long expiration = jwtService.extractExpiration(token).getTime();
        Long now = System.currentTimeMillis();
        Long ttl = expiration - now;
        
        if (ttl > 0) {
            redisTemplate.opsForValue().set("BL_" + token, email, ttl, TimeUnit.MILLISECONDS);
        }
    }

    public record TwoFactorAuthSetupResponse(String secretKey, String qrCodeImageUri) {}

    public TwoFactorAuthSetupResponse generateMfaSecret(String email) {
        // Generate a new secret
        String secret = generateSecret();
        
        // Generate QR code
        String qrCodeImageUri = generateQrCodeImageUri(email, secret);
        
        // Cache the secret temporarily until the user completes setup
        redisTemplate.opsForValue().set("MFA_SETUP_" + email, secret, 10, TimeUnit.MINUTES);
        
        return new TwoFactorAuthSetupResponse(secret, qrCodeImageUri);
    }

    @Transactional
    public void enableMfa(String email, String mfaCode) {
        // Get the user from the repository
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Get the temporary secret from Redis
        String secret = redisTemplate.opsForValue().get("MFA_SETUP_" + email);
        if (secret == null) {
            throw new RuntimeException("MFA setup expired, please try again");
        }
        
        // Verify the MFA code
        if (!verifyCode(mfaCode, secret)) {
            throw new RuntimeException("Invalid MFA code");
        }
        
        // Update the user with MFA enabled
        user.setMfaEnabled(true);
        user.setMfaSecret(secret);
        userRepository.save(user);
        
        // Remove the temporary secret from Redis
        redisTemplate.delete("MFA_SETUP_" + email);
    }

    @Transactional
    public void disableMfa(String email, String password) {
        // Get the user from the repository
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Verify the password
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }
        
        // Update the user with MFA disabled
        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        userRepository.save(user);
    }

    // Helper methods
    private String generateSecret() {
        SecretGenerator secretGenerator = new DefaultSecretGenerator();
        return secretGenerator.generate();
    }

    private String generateQrCodeImageUri(String email, String secret) {
        QrData qrData = new QrData.Builder()
                .label(email)
                .secret(secret)
                .issuer("Crime Reporting System")
                .algorithm(QrData.Algorithm.SHA1)
                .digits(6)
                .period(30)
                .build();

        QrGenerator qrGenerator = new ZxingPngQrGenerator();
        byte[] imageData;
        try {
            imageData = qrGenerator.generate(qrData);
        } catch (Exception e) {
            throw new RuntimeException("Error generating QR code", e);
        }

        return getDataUriForImage(imageData, qrGenerator.getImageMimeType());
    }

    private boolean verifyCode(String code, String secret) {
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
        return verifier.isValidCode(secret, code);
    }

    private Map<String, Object> createClaims(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("firstName", user.getFirstName());
        claims.put("lastName", user.getLastName());
        claims.put("email", user.getEmail());
        claims.put("role", user.getRole());
        claims.put("mfaEnabled", user.isMfaEnabled());
        return claims;
    }
} 