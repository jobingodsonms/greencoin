package com.greencoin.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String firebaseUid;

    @Column(unique = true, nullable = false)
    private String email;

    private String displayName;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.CITIZEN;

    @Builder.Default
    private Integer coinBalance = 0;

    private String profileImageUrl;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public boolean hasRole(UserRole role) {
        return this.role == role;
    }

    public enum UserRole {
        CITIZEN,
        COLLECTOR,
        AUTHORITY,
        ADMIN
    }

}
