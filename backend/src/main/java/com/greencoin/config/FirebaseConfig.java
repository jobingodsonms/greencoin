package com.greencoin.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.credentials-path:firebase-admin-key.json}")
    private String credentialsPath;

    @Value("${firebase.project-id}")
    private String projectId;

    @Bean
    public FirebaseApp firebaseApp() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                InputStream serviceAccount;

                // Try environment variable first
                String firebaseKey = System.getenv("FIREBASE_KEY_BASE64");
                if (firebaseKey != null && !firebaseKey.isBlank()) {
                    String trimmedKey = firebaseKey.trim();
                    byte[] keyBytes;

                    if (trimmedKey.startsWith("{")) {
                        log.info("Initializing Firebase using Raw JSON environment variable");
                        keyBytes = trimmedKey.getBytes();
                    } else {
                        log.info("Initializing Firebase using Base64 environment variable");
                        try {
                            keyBytes = Base64.getDecoder().decode(trimmedKey);
                        } catch (IllegalArgumentException e) {
                            log.error("Failed to decode FIREBASE_KEY_BASE64. Ensure it is a valid Base64 string.");
                            throw e;
                        }
                    }
                    serviceAccount = new ByteArrayInputStream(keyBytes);
                } else {
                    // Fallback to file path
                    log.info("Initializing Firebase using file: {}", credentialsPath);
                    serviceAccount = new FileInputStream(credentialsPath);
                }

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .setProjectId(projectId)
                        .build();

                FirebaseApp app = FirebaseApp.initializeApp(options);
                log.info("Firebase Admin SDK initialized successfully");
                return app;
            } else {
                return FirebaseApp.getInstance();
            }
        } catch (IOException e) {
            log.error("IO Error initializing Firebase Admin SDK: {}", e.getMessage());
            if (e.getMessage() != null && e.getMessage().contains("Invalid escape sequence")) {
                log.error(
                        "CRITICAL: The Firebase JSON key has a malformed 'private_key' field (Invalid escape sequence).");
                log.error("This usually happens when backslashes in the private key are not properly handled.");
                log.error("FIX: Re-generate your Base64 string using: base64 -w 0 your-key.json");
            }
            throw new RuntimeException("Failed to initialize Firebase due to IO/JSON error", e);
        } catch (Exception e) {
            log.error("Unexpected error initializing Firebase: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize Firebase", e);
        }
    }
}
