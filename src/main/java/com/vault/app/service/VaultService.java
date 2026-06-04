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
    public void updatePassword(String username, Long passwordId, PasswordRequestDTO request) {
        // 1. Authenticate the user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. Find the specific password AND verify this user owns it
        SavedPassword existingPassword = passwordRepository.findByIdAndUserId(passwordId, user.getId())
                .orElseThrow(() -> new RuntimeException("Password entry not found or unauthorized"));

        // 3. Update the details and encrypt the new password
        existingPassword.setPlatform(request.getPlatform());
        existingPassword.setLoginUsername(request.getLoginUsername());
        existingPassword.setEncryptedPassword(encryptionUtil.encrypt(request.getPassword()));

        // 4. Save the changes
        passwordRepository.save(existingPassword);
    }

    public void deletePassword(String username, Long passwordId) {
        // 1. Authenticate the user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. Find the password and verify ownership
        SavedPassword existingPassword = passwordRepository.findByIdAndUserId(passwordId, user.getId())
                .orElseThrow(() -> new RuntimeException("Password entry not found or unauthorized"));

        // 3. Delete it forever
        passwordRepository.delete(existingPassword);
    }
}