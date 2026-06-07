package com.drawai.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

    /**
     * @author specdock
     */

@SpringBootApplication(scanBasePackages = "com.drawai")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class);
    }
}
