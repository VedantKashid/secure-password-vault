package com.vault.app.config;

import com.vault.app.entity.User;
import com.vault.app.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Injecting Spring Security's built-in encoder instead!
    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Only create the user if they don't already exist
        if (userRepository.findByUsername("admin").isEmpty()) {
            User testUser = new User();
            testUser.setUsername("admin");
            testUser.setEmail("admin@vault.com");

            // Hash the password using the built-in encoder
            testUser.setPasswordHash(passwordEncoder.encode("SuperSecretPassword123"));

            userRepository.save(testUser);
            System.out.println("✅ [DEV] Test user 'admin' automatically generated!");
        }
    }
}