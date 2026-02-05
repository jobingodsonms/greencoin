package com.greencoin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Waste Report Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WasteReportResponse {
    private Long id;
    private Long reporterId;
    private String reporterName;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String imageUrl;
    private String description;
    private String status;
    private Integer coinsAwarded;
    private Long collectorId;
    private String collectorName;
    private LocalDateTime reportedAt;
    private LocalDateTime collectedAt;
}
