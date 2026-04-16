package com.FormFlow.FormFlow.Controller.HealthCheck;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class healthCheck {
    @GetMapping
    public String checkHealth() {
        return "FormFlow API is up and running!";
    }
}
