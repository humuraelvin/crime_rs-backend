package com.crime.reporting.crime_reporting_backend.dto.response;

import com.crime.reporting.crime_reporting_backend.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private Role role;
    private boolean mfaEnabled;
    private boolean mfaRequired;
} 