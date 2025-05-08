package com.crime.reporting.crime_reporting_backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String requestURI = request.getRequestURI();
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        log.debug("Processing request to URI: {}", requestURI);
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No Bearer token found in request to: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        log.debug("JWT token found in request to: {}", requestURI);
        
        try {
            userEmail = jwtService.extractUsername(jwt);
            log.debug("Extracted username from token: {}", userEmail);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                log.debug("Loaded user details for '{}' with authorities: {}", userEmail, userDetails.getAuthorities());
                
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    log.debug("Token is valid for user: {}", userEmail);
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("Authentication set in SecurityContext for user: {}", userEmail);
                } else {
                    log.warn("Token validation failed for user: {}", userEmail);
                }
            } else {
                if (userEmail == null) {
                    log.warn("Could not extract username from token");
                } else {
                    log.debug("Authentication already exists in SecurityContext");
                }
            }
        } catch (Exception e) {
            log.error("JWT token processing error: {}", e.getMessage(), e);
        }
        
        filterChain.doFilter(request, response);
    }
} 