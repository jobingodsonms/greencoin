package com.greencoin.controller;

import com.greencoin.dto.UserProfileResponse;
import com.greencoin.model.User;
import com.greencoin.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public ResponseEntity<UserProfileResponse> registerUser(
            Authentication authentication,
            @RequestParam(required = false) String displayName) {

        String firebaseUid = authentication.getName();

        // Extract email from authentication (set by FirebaseTokenFilter)
        String email = (String) authentication.getCredentials();

        User user = userService.getOrCreateUser(firebaseUid, email, displayName);
        UserProfileResponse response = mapToProfileResponse(user);

        return ResponseEntity.ok(response);
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
