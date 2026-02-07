package com.greencoin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {
    public static void main(String[] args) {
        String dbUrl = System.getenv("DATABASE_URL");
        if (dbUrl != null && !dbUrl.startsWith("jdbc:")) {
            System.setProperty("spring.datasource.url", "jdbc:" + dbUrl);
            // Also explicitly set username/password if present in URL (sometimes helpful)
            try {
                java.net.URI uri = new java.net.URI(dbUrl);
                String userInfo = uri.getUserInfo();
                if (userInfo != null && userInfo.contains(":")) {
                    System.setProperty("spring.datasource.username", userInfo.split(":")[0]);
                    System.setProperty("spring.datasource.password", userInfo.split(":")[1]);
                }
            } catch (Exception ignored) {
            }
        }
        SpringApplication.run(BackendApplication.class, args);
    }
}
