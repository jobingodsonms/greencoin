package com.greencoin.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "waste_reports")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WasteReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "reporter_id")
    private User reporter;

    @Column(nullable = false)
    private java.math.BigDecimal latitude;

    @Column(nullable = false)
    private java.math.BigDecimal longitude;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private ReportStatus status = ReportStatus.OPEN;

    @Builder.Default
    private Integer coinsAwarded = 0;

    @ManyToOne
    @JoinColumn(name = "collector_id")
    private User collector;

    @Builder.Default
    private LocalDateTime reportedAt = LocalDateTime.now();
    private LocalDateTime pickedAt;
    private LocalDateTime collectedAt;

    public enum ReportStatus {
        OPEN,
        PICKING,
        COLLECTED,
        REJECTED
    }
}
