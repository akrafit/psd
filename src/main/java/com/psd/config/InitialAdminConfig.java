package com.psd.config;

import com.psd.entity.User;
import com.psd.enums.UserRole;
import com.psd.repo.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Configuration
public class InitialAdminConfig {

    @Bean
    public CommandLineRunner createInitialAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            if (userRepository.count() == 0) {
                User admin = new User();
                admin.setEmail("admin@local");
                admin.setName("Администратор");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setEnabled(true);
                admin.setRole(UserRole.ADMIN);
                admin.setCreatedAt(LocalDateTime.now(ZoneId.of("Asia/Shanghai")));

                userRepository.save(admin);
            }
        };
    }
}