package com.vault.app.service;

import com.vault.app.dto.UserRegistrationDTO;
import com.vault.app.entity.User;
import com.vault.app.repository.UserRepository;
import com.vault.app.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public User registerUser(UserRegistrationDTO registrationDTO) {
        // 1. Check if username or email already exists
        if (userRepository.findByUsername(registrationDTO.getUsername()).isPresent()) {
            throw new RuntimeException("Username is already taken!");
        }
        if (userRepository.findByEmail(registrationDTO.getEmail()).isPresent()) {
            throw new RuntimeException("Email is already registered!");
        }

        // 2. Map the DTO to our User Entity
        User newUser = new User();
        newUser.setUsername(registrationDTO.getUsername());
        newUser.setEmail(registrationDTO.getEmail());

        // 3. Hash the password before saving
        newUser.setPasswordHash(passwordEncoder.encode(registrationDTO.getPassword()));

        // 4. Save to the database
        return userRepository.save(newUser);
    }
    public String login(com.vault.app.dto.UserLoginDTO loginDTO) {

        System.out.println("🚨 USERNAME RECEIVED: [" + loginDTO.getUsername() + "]");
        System.out.println("🚨 PASSWORD RECEIVED: [" + loginDTO.getPassword() + "]");

        // 1. Find the user
        User user = userRepository.findByUsername(loginDTO.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found!"));

        System.out.println("🚨 DB HASH FOUND: " + user.getPasswordHash());

        // 2. See if the password encoder actually matches them
        boolean isMatch = passwordEncoder.matches(loginDTO.getPassword(), user.getPasswordHash());
        System.out.println("🚨 DID PASSWORD MATCH? " + isMatch);

        if (!isMatch) {
            throw new RuntimeException("Invalid password!");
        }

        // 3. If it matched, try to generate the token!
        System.out.println("🚨 PASSWORD CORRECT! ATTEMPTING TO GENERATE JWT...");
        try {
            String token = jwtUtil.generateToken(user.getUsername());
            System.out.println("🚨 JWT GENERATED SUCCESSFULLY!");
            return token;
        } catch (Exception e) {
            System.out.println("🚨 CRASH! JWT GENERATOR FAILED: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("JWT Crash: " + e.getMessage());
        }
    }
}