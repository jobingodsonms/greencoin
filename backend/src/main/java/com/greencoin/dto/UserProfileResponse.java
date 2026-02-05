package com.greencoin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User Profile Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String firebaseUid;
    private String email;
    private String displayName;
    private String role;
    private Integer coinBalance;
    private String profileImageUrl;
}
