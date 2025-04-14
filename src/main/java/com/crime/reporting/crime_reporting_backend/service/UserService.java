package com.crime.reporting.crime_reporting_backend.service;

import com.crime.reporting.crime_reporting_backend.dto.UserResponse;
import com.crime.reporting.crime_reporting_backend.dto.UpdateUserProfileRequest;

import java.util.List;

public interface UserService {
    
    /**
     * Enables MFA for a user
     * @param email the user's email
     * @param secret the MFA secret
     */
    void enableMfa(String email, String secret);
    
    /**
     * Disables MFA for a user
     * @param email the user's email
     */
    void disableMfa(String email);

    /**
     * Updates a user's profile
     * @param userId the user ID
     * @param updateUserProfileRequest the request with updated profile data
     * @return updated user data
     */
    UserResponse updateUserProfile(Long userId, UpdateUserProfileRequest updateUserProfileRequest);
    
    /**
     * Gets all users
     * @return a list of all users
     */
    List<UserResponse> getAllUsers();
    
    /**
     * Deletes a user
     * @param userId the user ID to delete
     */
    void deleteUser(Long userId);
    
    /**
     * Gets a user by ID
     * @param id the user ID
     * @return the user data
     */
    UserResponse getUserById(Long id);
    
    /**
     * Finds a user by their email address
     * @param email the email address to search for
     * @return the user data
     */
    UserResponse findByEmail(String email);
} 