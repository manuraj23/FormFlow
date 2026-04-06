package com.FormFlow.FormFlow.Service.Email;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class ResetPasswordEmailService {
    @Autowired
    private JavaMailSender mailSender;

    public void sendOtp(String toEmail, String otp) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Reset Password OTP - FormFlow");
        message.setText(
                "Your OTP for reset password is: " + otp +
                        "\n\nThis OTP will expire in 10 minutes." +
                        "\n\nDo not share this OTP with anyone."
        );
        mailSender.send(message);
    }
}
