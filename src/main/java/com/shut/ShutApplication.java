package com.shut;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ShutApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShutApplication.class, args);
    }
}
