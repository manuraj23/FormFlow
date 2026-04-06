package com.FormFlow.FormFlow.Entity.UserEntity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "password_reset_temp")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetTemp {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String email;
    private String otp;
    private int otpAttempts;
    private LocalDateTime otpExpiry;
    private boolean isVerified;
}