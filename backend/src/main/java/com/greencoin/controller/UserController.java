package com.greencoin.controller;

import com.greencoin.dto.UserProfileResponse;
import com.greencoin.dto.ErrorResponse;
import com.greencoin.model.User;
import com.greencoin.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * User & Authentication Controller
 * 
 * Endpoints:
 * - GET /api/user/profile - Get current user profile
 * - POST /api/user/register - Create/sync user on first login
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Get current user's profile
     * Authentication: Firebase UID from JWT
     */
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(Authentication authentication) {
        String firebaseUid = authentication.getName();
        User user = userService.getUserByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserProfileResponse response = mapToProfileResponse(user);
        return ResponseEntity.ok(response);
    }

    /**
     * Register/sync user on first login
     * Frontend calls this after Firebase authentication succeeds
     * 
     * @param displayName - Optional display name from Firebase
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(
            Authentication authentication,
            @RequestParam(required = false) String displayName) {

        if (authentication == null) {
            log.error("Registration failed: Authentication is null for request to /register");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Authentication failed. No valid token found in request."));
        }

        try {
            log.info("Registering/Syncing user. Principal: {}, Authenticated: {}",
                    authentication.getName(), authentication.isAuthenticated());
            String firebaseUid = authentication.getName();
            String email = (String) authentication.getCredentials();

            log.info("Registering/Syncing user: {} (UID: {})", email, firebaseUid);
            User user = userService.getOrCreateUser(firebaseUid, email, displayName);
            UserProfileResponse response = mapToProfileResponse(user);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error during user registration: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Registration failed: " + e.getMessage()));
        }
    }

    /**
     * Map User entity to DTO
     */
    private UserProfileResponse mapToProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .firebaseUid(user.getFirebaseUid())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .role(user.getRole().name())
                .coinBalance(user.getCoinBalance())
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }
}
