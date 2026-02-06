package com.greencoin.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
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

    @PostConstruct
    public void initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                InputStream serviceAccount;

                // Try Base64 string first (ideal for Railway/production)
                String base64Key = System.getenv("FIREBASE_KEY_BASE64");
                if (base64Key != null && !base64Key.isBlank()) {
                    log.info("Initializing Firebase using Base64 environment variable");
                    byte[] decodedKey = Base64.getDecoder().decode(base64Key.trim());
                    serviceAccount = new ByteArrayInputStream(decodedKey);
                } else {
                    // Fallback to file path
                    log.info("Initializing Firebase using file: {}", credentialsPath);
                    serviceAccount = new FileInputStream(credentialsPath);
                }

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .setProjectId(projectId)
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("Firebase Admin SDK initialized successfully");
            }
        } catch (IOException e) {
            log.error("Error initializing Firebase Admin SDK: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("Error decoding FIREBASE_KEY_BASE64: {}", e.getMessage());
        }
    }
}
