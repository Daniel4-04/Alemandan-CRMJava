package com.alemandan.crm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AlemandanCrmJavaApplication {

    public static void main(String[] args) {
        // Force headless mode to prevent native library loading issues on Railway
        // This prevents UnsatisfiedLinkError for libfreetype.so.6 when generating charts
        System.setProperty("java.awt.headless", "true");
        
        SpringApplication.run(AlemandanCrmJavaApplication.class, args);
    }

}
