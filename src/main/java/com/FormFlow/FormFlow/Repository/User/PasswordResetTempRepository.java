package com.FormFlow.FormFlow.Repository.User;

import com.FormFlow.FormFlow.Entity.UserEntity.PasswordResetTemp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTempRepository extends JpaRepository<PasswordResetTemp, UUID> {

    Optional<PasswordResetTemp> findByEmail(String email);

    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetTemp p WHERE p.email = :email")
    void deleteByEmail(String email);

    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetTemp p WHERE p.otpExpiry < :currentTime")
    void deleteByOtpExpiry(java.time.LocalDateTime currentTime);

    boolean existsByEmail(String email);
}
