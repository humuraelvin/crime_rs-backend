package com.crime.reporting.crime_reporting_backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class ReportWebConfig implements WebMvcConfigurer {
    private static final Logger log = LoggerFactory.getLogger(ReportWebConfig.class);

    /*
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new ReportCorsInterceptor())
                .addPathPatterns("/api/v1/reports/**");
    }

    private static class ReportCorsInterceptor implements HandlerInterceptor {
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            log.info("Report interceptor handling request: {}", request.getRequestURI());
            
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200");
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS, PATCH");
            response.setHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "Origin, X-Requested-With, Content-Type, Accept, Authorization, Cache-Control");
            response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Content-Disposition");
            
            return true;
        }
    }
    */
} 