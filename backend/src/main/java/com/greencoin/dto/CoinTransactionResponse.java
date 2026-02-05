package com.greencoin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Coin Transaction Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoinTransactionResponse {
    private Long id;
    private Integer amount;
    private String transactionType;
    private Long referenceId;
    private String referenceType;
    private LocalDateTime createdAt;
}
