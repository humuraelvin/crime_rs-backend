package com.crime.reporting.crime_reporting_backend.service;

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
import org.springframework.stereotype.Service;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

@Service
public class MfaService {

    /**
     * Generates a new secret key for MFA
     * @return the generated secret
     */
    public String generateSecret() {
        SecretGenerator secretGenerator = new DefaultSecretGenerator();
        return secretGenerator.generate();
    }
    
    /**
     * Generates a QR code image URI for MFA setup
     * @param email the user's email
     * @param secret the MFA secret
     * @return a data URI for the QR code image
     */
    public String generateQrCodeImageUri(String email, String secret) {
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
    
    /**
     * Validates a TOTP code against a secret
     * @param secret the MFA secret
     * @param code the TOTP code to validate
     * @return true if the code is valid, false otherwise
     */
    public boolean validateTotp(String secret, String code) {
        CodeVerifier verifier = new DefaultCodeVerifier(
                new DefaultCodeGenerator(),
                new SystemTimeProvider()
        );
        
        return verifier.isValidCode(secret, code);
    }
} 