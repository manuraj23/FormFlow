package com.FormFlow.FormFlow.Schedular;
import com.FormFlow.FormFlow.Repository.User.PasswordResetTempRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class RemovePasswordResetTempRepo {
    @Autowired
    private PasswordResetTempRepository passwordResetTempRepository;

    @Scheduled(cron = "0 */5 * * * *")
    public void removePasswordResetTempRepo() {
        passwordResetTempRepository.deleteByOtpExpiry(LocalDateTime.now());
        System.out.println("Expired password reset tokens removed at: " + LocalDateTime.now());
    }
}
