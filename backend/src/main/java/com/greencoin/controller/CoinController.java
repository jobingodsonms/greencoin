package com.greencoin.controller;

import com.greencoin.dto.CoinTransactionResponse;
import com.greencoin.model.CoinTransaction;
import com.greencoin.model.User;
import com.greencoin.service.CoinService;
import com.greencoin.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Coin & Transaction Controller
 * 
 * Endpoints for viewing coin balance and transaction history.
 */
@Slf4j
@RestController
@RequestMapping("/api/coins")
@RequiredArgsConstructor
public class CoinController {

    private final CoinService coinService;
    private final UserService userService;

    /**
     * Get current user's coin balance
     */
    @GetMapping("/balance")
    public ResponseEntity<Map<String, Object>> getBalance(Authentication authentication) {
        String firebaseUid = authentication.getName();
        User user = userService.getUserByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> response = new HashMap<>();
        response.put("balance", user.getCoinBalance());
        response.put("userId", user.getId());

        return ResponseEntity.ok(response);
    }

    /**
     * Get current user's transaction history
     */
    @GetMapping("/transactions")
    public ResponseEntity<List<CoinTransactionResponse>> getTransactions(Authentication authentication) {
        String firebaseUid = authentication.getName();
        User user = userService.getUserByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<CoinTransaction> transactions = coinService.getTransactionHistory(user.getId());
        List<CoinTransactionResponse> response = transactions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Map entity to DTO
     */
    private CoinTransactionResponse mapToResponse(CoinTransaction transaction) {
        return CoinTransactionResponse.builder()
                .id(transaction.getId())
                .amount(transaction.getAmount())
                .transactionType(transaction.getTransactionType().name())
                .referenceId(transaction.getReferenceId())
                .referenceType(transaction.getReferenceType())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
