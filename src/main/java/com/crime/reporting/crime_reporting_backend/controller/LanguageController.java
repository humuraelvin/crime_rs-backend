package com.crime.reporting.crime_reporting_backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

@RestController
@RequestMapping("/api/v1/languages")
public class LanguageController {

    @Autowired
    private MessageSource messageSource;

    @GetMapping
    public ResponseEntity<Map<String, String>> getAvailableLanguages() {
        Map<String, String> languages = new HashMap<>();
        languages.put("en", "English");
        languages.put("fr", "French");
        languages.put("rw", "Kinyarwanda");
        return ResponseEntity.ok(languages);
    }

    @GetMapping("/translations")
    public ResponseEntity<Map<String, String>> getTranslations(
            @RequestParam(defaultValue = "en") String lang) {
        
        // Create locale from the language code
        Locale locale = new Locale(lang);
        
        // Load resource bundle for the locale
        ResourceBundle bundle = ResourceBundle.getBundle("messages", locale);
        
        // Convert resource bundle to a map
        Map<String, String> translations = new HashMap<>();
        bundle.keySet().forEach(key -> 
            translations.put(key, bundle.getString(key))
        );
        
        return ResponseEntity.ok(translations);
    }
} 