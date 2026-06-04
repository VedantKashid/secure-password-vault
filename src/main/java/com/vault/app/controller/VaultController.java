package com.vault.app.controller;

import com.vault.app.dto.PasswordRequestDTO;
import com.vault.app.entity.SavedPassword;
import com.vault.app.service.VaultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vault")
@RequiredArgsConstructor
public class VaultController {

    private final VaultService vaultService;

    // Add a new password to the vault
    @PostMapping("/{userId}/add")
    public ResponseEntity<String> addPassword(@PathVariable Long userId, @Valid @RequestBody PasswordRequestDTO request) {
        try {
            vaultService.savePassword(userId, request);
            return new ResponseEntity<>("Password saved securely!", HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Get all passwords for a specific user
    @GetMapping("/{userId}/all")
    public ResponseEntity<List<SavedPassword>> getAllPasswords(@PathVariable Long userId) {
        return ResponseEntity.ok(vaultService.getUserPasswords(userId));
    }
}