package com.talan.creditplatform.config;

import com.talan.creditplatform.model.entity.User;
import com.talan.creditplatform.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DatabaseInitializer {

    @Bean
    public CommandLineRunner initDatabase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // Upsert each default user so they always exist with the correct password,
            // even if the sample_data.sql was run and wiped the users table.
            upsertUser(userRepository, passwordEncoder, "admin",   "adminpass",  "admin");
            upsertUser(userRepository, passwordEncoder, "manager", "managerpass", "manager");
            upsertUser(userRepository, passwordEncoder, "banker",  "bankerpass", "manager");
            upsertUser(userRepository, passwordEncoder, "analyst", "analystpass","analyst");
        };
    }

    private void upsertUser(UserRepository repo, PasswordEncoder encoder,
                            String username, String rawPassword, String role) {
        repo.findByUsername(username).ifPresentOrElse(
            existing -> {
                // Re-encode and update password in case it was corrupted by manual SQL
                existing.setPassword(encoder.encode(rawPassword));
                existing.setRole(role);
                repo.save(existing);
            },
            () -> repo.save(new User(username, encoder.encode(rawPassword), role))
        );
    }
}

