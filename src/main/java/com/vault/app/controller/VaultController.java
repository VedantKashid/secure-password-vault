package com.vault.app.controller;

import com.vault.app.dto.PasswordRequestDTO;
import com.vault.app.entity.SavedPassword;
import com.vault.app.service.VaultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/vault")
@RequiredArgsConstructor
public class VaultController {

    private final VaultService vaultService;

    // Notice we removed /{userId} from the URL!
    @PostMapping("/add")
    public ResponseEntity<String> addPassword(Principal principal, @Valid @RequestBody PasswordRequestDTO request) {
        try {
            // principal.getName() automatically extracts the username from the verified JWT
            vaultService.savePassword(principal.getName(), request);
            return new ResponseEntity<>("Password saved securely!", HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<SavedPassword>> getAllPasswords(Principal principal) {
        return ResponseEntity.ok(vaultService.getUserPasswords(principal.getName()));
    }
    // Update an existing password
    @PutMapping("/{passwordId}")
    public ResponseEntity<String> updatePassword(Principal principal, @PathVariable Long passwordId, @Valid @RequestBody PasswordRequestDTO request) {
        try {
            vaultService.updatePassword(principal.getName(), passwordId, request);
            return ResponseEntity.ok("Password updated successfully!");
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Delete a password
    @DeleteMapping("/{passwordId}")
    public ResponseEntity<String> deletePassword(Principal principal, @PathVariable Long passwordId) {
        try {
            vaultService.deletePassword(principal.getName(), passwordId);
            return ResponseEntity.ok("Password deleted successfully!");
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
    // Search passwords by platform keyword
    @GetMapping("/search")
    public ResponseEntity<List<SavedPassword>> searchPasswords(Principal principal, @RequestParam String keyword) {
        try {
            List<SavedPassword> results = vaultService.searchPasswords(principal.getName(), keyword);
            return ResponseEntity.ok(results);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }
}