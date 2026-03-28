package com.FormFlow.FormFlow.Repository;

import com.FormFlow.FormFlow.Entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    void deleteByUser_UserId(Long userId);

    void deleteByExpiryDateBefore(Instant now);

    List<RefreshToken> findByExpiryDateBefore(Instant now);
}
