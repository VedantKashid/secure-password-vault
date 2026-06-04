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

    public void savePassword(Long userId, PasswordRequestDTO request) {
        // 1. Find the user making the request
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. Create a new SavedPassword entity
        SavedPassword newPassword = new SavedPassword();
        newPassword.setPlatform(request.getPlatform());
        newPassword.setLoginUsername(request.getLoginUsername());

        // 3. Encrypt the actual password using AES
        String encrypted = encryptionUtil.encrypt(request.getPassword());
        newPassword.setEncryptedPassword(encrypted);

        // 4. Link it to the user and save
        newPassword.setUser(user);
        passwordRepository.save(newPassword);
    }

    public List<SavedPassword> getUserPasswords(Long userId) {
        // Fetch all passwords for this user
        List<SavedPassword> vault = passwordRepository.findByUserId(userId);

        // Decrypt them so the user can read them
        vault.forEach(p -> p.setEncryptedPassword(encryptionUtil.decrypt(p.getEncryptedPassword())));

        return vault;
    }
}