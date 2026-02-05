package com.greencoin.service;

import com.greencoin.model.CoinTransaction;
import com.greencoin.model.User;
import com.greencoin.repository.CoinTransactionRepository;
import com.greencoin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CoinService {

    private final CoinTransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Transactional
    public void awardCoins(User user, Integer amount, Long reportId) {
        user.setCoinBalance(user.getCoinBalance() + amount);
        userRepository.save(user);

        CoinTransaction tx = CoinTransaction.builder()
                .user(user)
                .amount(amount)
                .transactionType(CoinTransaction.TransactionType.EARNED)
                .referenceId(reportId)
                .referenceType("WASTE_REPORT")
                .createdAt(LocalDateTime.now())
                .build();
        transactionRepository.save(tx);
    }

    @Transactional
    public void redeemCoins(User user, Integer amount, String item) {
        if (user.getCoinBalance() < amount) {
            throw new RuntimeException("Insufficient balance");
        }

        user.setCoinBalance(user.getCoinBalance() - amount);
        userRepository.save(user);

        CoinTransaction tx = CoinTransaction.builder()
                .user(user)
                .amount(-amount)
                .transactionType(CoinTransaction.TransactionType.REDEEMED)
                .referenceType("MARKETPLACE_REDEEM")
                .referenceId(0L) // Reference to actual item could be added
                .createdAt(LocalDateTime.now())
                .build();
        transactionRepository.save(tx);
    }

    public List<CoinTransaction> getTransactionHistory(Long userId) {
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
