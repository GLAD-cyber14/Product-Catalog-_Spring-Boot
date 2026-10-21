package com.gladwin.userservice.productsdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ProductsDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductsDemoApplication.class, args);
    }

    @Bean
    ApplicationRunner checkDatabaseConfig(Environment env) {
        return args -> {
            System.out.println("ACTIVE PROFILES: "
                    + String.join(", ", env.getActiveProfiles()));
            System.out.println("DATABASE USER: "
                    + env.getProperty("spring.datasource.username"));
        };
    }
}