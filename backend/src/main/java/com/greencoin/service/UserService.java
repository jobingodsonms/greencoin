package com.greencoin.service;

import com.greencoin.model.User;
import com.greencoin.repository.UserRepository;
import com.greencoin.repository.CollectorWhitelistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CollectorWhitelistRepository whitelistRepository;

    public Optional<User> getUserByFirebaseUid(String firebaseUid) {
        return userRepository.findByFirebaseUid(firebaseUid);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    public User getOrCreateUser(String firebaseUid, String email, String displayName) {
        return syncUserWithFirebase(firebaseUid, email, displayName, null);
    }

    @Transactional
    public User syncUserWithFirebase(String firebaseUid, String email, String displayName, String photoUrl) {
        return userRepository.findByFirebaseUid(firebaseUid)
                .map(user -> {
                    user.setDisplayName(displayName);
                    user.setProfileImageUrl(photoUrl);
                    return userRepository.save(user);
                })
                .orElseGet(() -> {
                    User.UserRole role = whitelistRepository.existsByEmail(email) ? User.UserRole.COLLECTOR
                            : User.UserRole.CITIZEN;

                    User newUser = User.builder()
                            .firebaseUid(firebaseUid)
                            .email(email)
                            .displayName(displayName)
                            .profileImageUrl(photoUrl)
                            .role(role)
                            .coinBalance(0)
                            .build();
                    log.info("Registering new user: {} with role: {}", email, role);
                    return userRepository.save(newUser);
                });
    }

    @Transactional
    public void updateCoinBalance(Long userId, Integer amount) {
        User user = getUserById(userId);
        user.setCoinBalance(user.getCoinBalance() + amount);
        userRepository.save(user);
    }
}
