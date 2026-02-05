package com.greencoin.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "collector_whitelist")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectorWhitelist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    private String addedBy;
    @Builder.Default
    private LocalDateTime addedAt = LocalDateTime.now();
}
