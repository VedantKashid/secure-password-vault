package com.vault.app.service;

import com.vault.app.dto.UserRegistrationDTO;
import com.vault.app.entity.User;
import com.vault.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
}