package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.example")
public class EnhancedResumeApplication {
    public static void main(String[] args) {
        SpringApplication.run(EnhancedResumeApplication.class, args);
    }
}