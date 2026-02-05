package com.greencoin.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "coin_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoinTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private Integer amount;

    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    private Long referenceId;
    private String referenceType;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum TransactionType {
        EARNED,
        REDEEMED
    }
}
