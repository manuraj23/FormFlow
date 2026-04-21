package com.FormFlow.FormFlow.Controller.HealthCheck;

import com.FormFlow.FormFlow.Service.Email.SendGridEmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class healthCheck {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @GetMapping
    public String checkHealth() {
        return "FormFlow API is up and running!";
    }

    @GetMapping("/mailTestGmail")
    public ResponseEntity<String> mailTest() {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo("manuraj0642@gmail.com");
            message.setSubject("Mail test - FormFlow");
            message.setText(
                    "Email test runs fine "
            );
            mailSender.send(message);
            return ResponseEntity.ok("Mail test sent successfully.");
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Mail test failed: " + ex.getMessage());
        }
    }

    @Autowired
    public SendGridEmailService sendGridEmailService;

    @GetMapping("/mailTest")
    public ResponseEntity<String> sendOtp() {
        try {
            sendGridEmailService.sendMail();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Mail test failed: " + ex.getMessage());
        }
        return ResponseEntity.ok("Mail test sent successfully.1");
    }

}
