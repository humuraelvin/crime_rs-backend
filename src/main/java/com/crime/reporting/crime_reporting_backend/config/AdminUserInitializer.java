package com.crime.reporting.crime_reporting_backend.config;

import com.crime.reporting.crime_reporting_backend.entity.Role;
import com.crime.reporting.crime_reporting_backend.entity.User;
import com.crime.reporting.crime_reporting_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AdminUserInitializer {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Bean
    public CommandLineRunner initializeAdminUser() {
        return args -> {
            // Check if admin user already exists
            if (userRepository.existsByEmail("admin@crimereporting.com")) {
                log.info("Admin user already exists, skipping initialization");
                return;
            }
            
            log.info("Creating admin user...");
            
            // Create admin user
            User adminUser = User.builder()
                    .firstName("Admin")
                    .lastName("User")
                    .email("admin@crimereporting.com")
                    .password(passwordEncoder.encode("Admin@123"))
                    .role(Role.ADMIN)
                    .phoneNumber("+1234567890")
                    .mfaEnabled(false)
                    .build();
            
            userRepository.save(adminUser);
            
            log.info("Admin user created successfully!");
            log.info("Admin Credentials:");
            log.info("Email: admin@crimereporting.com");
            log.info("Password: Admin@123");
        };
    }
} 