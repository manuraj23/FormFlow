package com.FormFlow.FormFlow.Config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    private String uploadPath;

    @PostConstruct
    public void init() {
        // Convert to absolute path (works on ANY OS)
        Path path = Paths.get(uploadDir).toAbsolutePath().normalize();
        uploadPath = path.toUri().toString();

        System.out.println("Upload directory: " + uploadPath);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath);
    }
}