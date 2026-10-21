package com.gladwin.userservice.productsdemo.config;

import com.gladwin.userservice.productsdemo.Model.AppUser;
import com.gladwin.userservice.productsdemo.Repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminInitializer {

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Bean
    public CommandLineRunner createDefaultAdmin(AppUserRepository appUserRepository,
                                                PasswordEncoder passwordEncoder) {
        return args -> {
            if (appUserRepository.findByUsername(adminUsername).isEmpty()) {
                AppUser admin = new AppUser();
                admin.setUsername(adminUsername);
                admin.setPassword(passwordEncoder.encode(adminPassword));
                admin.setRole("ADMIN");

                appUserRepository.save(admin);
            }
        };
    }
}