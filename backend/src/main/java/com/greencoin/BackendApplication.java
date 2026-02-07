package com.greencoin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {
    public static void main(String[] args) {
        String dbUrl = System.getenv("DATABASE_URL");
        System.out.println("DEBUG: DATABASE_URL from environment: " + dbUrl);

        if (dbUrl != null && !dbUrl.isEmpty() && !dbUrl.startsWith("jdbc:")) {
            String jdbcUrl = "jdbc:" + dbUrl;
            System.out.println("DEBUG: Setting spring.datasource.url to: " + jdbcUrl);
            System.setProperty("spring.datasource.url", jdbcUrl);

            try {
                java.net.URI uri = new java.net.URI(dbUrl);
                String userInfo = uri.getUserInfo();
                if (userInfo != null && userInfo.contains(":")) {
                    String[] parts = userInfo.split(":");
                    System.setProperty("spring.datasource.username", parts[0]);
                    System.setProperty("spring.datasource.password", parts[1]);
                    System.out.println("DEBUG: Username/Password extracted from URL");
                }
            } catch (Exception e) {
                System.out.println("DEBUG: Could not parse database URL for credentials: " + e.getMessage());
            }
        }

        SpringApplication.run(BackendApplication.class, args);
    }
}
