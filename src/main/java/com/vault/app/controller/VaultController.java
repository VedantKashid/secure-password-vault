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
}