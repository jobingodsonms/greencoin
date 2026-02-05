package com.greencoin.repository;

import com.greencoin.model.CollectorWhitelist;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CollectorWhitelistRepository extends JpaRepository<CollectorWhitelist, Long> {
    Optional<CollectorWhitelist> findByEmail(String email);

    boolean existsByEmail(String email);
}
