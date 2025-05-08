package com.crime.reporting.crime_reporting_backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@Slf4j
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Get absolute path to upload directory
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        String uploadAbsolutePath = uploadPath.toString().replace("\\", "/");
        
        if (!uploadAbsolutePath.endsWith("/")) {
            uploadAbsolutePath += "/";
        }
        
        // Log the file location for debugging
        log.info("Serving files from: {}", uploadAbsolutePath);
        
        // Expose the uploads directory as /uploads/**
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadAbsolutePath)
                .setCachePeriod(3600); // Cache for 1 hour
        
        // Also expose a direct /files/** endpoint for simplified access
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + uploadAbsolutePath)
                .setCachePeriod(3600); // Cache for 1 hour
    }

    /*
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("http://localhost:4200")
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers")
                        .exposedHeaders("Content-Disposition")
                        .allowCredentials(true)
                        .maxAge(3600);
            }
        };
    }
    */
} 