package com.FormFlow.FormFlow.Schedular;
import com.FormFlow.FormFlow.Entity.UserEntity.TempUser;
import com.FormFlow.FormFlow.Repository.User.TempUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class TempUserCleanupScheduler {
    @Autowired
    private TempUserRepository tempUserRepository;
    @Scheduled(cron = "0 */5 * * * *")
    public void removeExpiredTempUsers() {
        List<TempUser> users = tempUserRepository.findAll();
        for(TempUser user : users){
            if(user.getOtpExpiry().isBefore(LocalDateTime.now())){
                tempUserRepository.delete(user);
            }
        }
    }
}