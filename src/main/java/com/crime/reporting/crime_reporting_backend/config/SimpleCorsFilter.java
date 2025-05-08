package com.crime.reporting.crime_reporting_backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Simple CORS filter that adds headers to all responses.
 * This filter has highest precedence to ensure it runs before security filters.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class SimpleCorsFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) res;
        HttpServletRequest request = (HttpServletRequest) req;
        
        // Log the request
        log.debug("Processing CORS filter for: {} {}", request.getMethod(), request.getRequestURI());
        
        // Add CORS headers to response
        response.setHeader("Access-Control-Allow-Origin", "http://localhost:4200");
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE, PUT");
        response.setHeader("Access-Control-Max-Age", "3600");
        response.setHeader("Access-Control-Allow-Headers", "x-requested-with, authorization, content-type, xsrf-token, cache-control");
        response.setHeader("Access-Control-Expose-Headers", "content-disposition");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        
        // Special handling for preflight (OPTIONS) requests
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            chain.doFilter(req, res);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) {
        // Nothing to initialize
    }

    @Override
    public void destroy() {
        // Nothing to clean up
    }
} 