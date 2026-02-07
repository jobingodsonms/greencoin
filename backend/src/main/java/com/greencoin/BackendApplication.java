package com.greencoin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = { org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class })
public class BackendApplication {
    public static void main(String[] args) {
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl != null && !databaseUrl.isEmpty() && !databaseUrl.startsWith("jdbc:")) {
            try {
                java.net.URI dbUri = new java.net.URI(databaseUrl);
                String userInfo = dbUri.getUserInfo();
                if (userInfo != null) {
                    String username = userInfo.split(":")[0];
                    String password = userInfo.split(":")[1];
                    String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ":" + dbUri.getPort() + dbUri.getPath();

                    System.setProperty("spring.datasource.url", dbUrl);
                    System.setProperty("spring.datasource.username", username);
                    System.setProperty("spring.datasource.password", password);
                }
            } catch (Exception e) {
                System.err.println("Error converting DATABASE_URL: " + e.getMessage());
            }
        }
        SpringApplication.run(BackendApplication.class, args);
    }
}
