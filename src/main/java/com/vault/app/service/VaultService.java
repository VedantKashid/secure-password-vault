package com.vault.app.service;

import com.vault.app.dto.PasswordRequestDTO;
import com.vault.app.entity.SavedPassword;
import com.vault.app.entity.User;
import com.vault.app.repository.SavedPasswordRepository;
import com.vault.app.repository.UserRepository;
import com.vault.app.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VaultService {

    private final SavedPasswordRepository passwordRepository;
    private final UserRepository userRepository;
    private final EncryptionUtil encryptionUtil;

    public void savePassword(String username, PasswordRequestDTO request) {
        // Find the user by their verified username from the token
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        SavedPassword newPassword = new SavedPassword();
        newPassword.setPlatform(request.getPlatform());
        newPassword.setLoginUsername(request.getLoginUsername());

        String encrypted = encryptionUtil.encrypt(request.getPassword());
        newPassword.setEncryptedPassword(encrypted);

        newPassword.setUser(user);
        passwordRepository.save(newPassword);
    }

    public List<SavedPassword> getUserPasswords(String username) {
        // Find the user first to get their ID
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Fetch all passwords for this user
        List<SavedPassword> vault = passwordRepository.findByUserId(user.getId());

        vault.forEach(p -> p.setEncryptedPassword(encryptionUtil.decrypt(p.getEncryptedPassword())));

        return vault;
    }
}