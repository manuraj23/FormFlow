package com.FormFlow.FormFlow.Schedular;
import com.FormFlow.FormFlow.Repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class RemoveExpiredRefreshToken {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "0 */10 * * * *")
    @Transactional
    public void removeExpiredTokens() {
        refreshTokenRepository.deleteByExpiryDateBefore(Instant.now());
        System.out.println("Expired refresh tokens removed at: " + Instant.now());
    }
}
