package com.vault.app.service;

import com.vault.app.dto.BreachScanResultDTO;
import com.vault.app.dto.BreachedPasswordDTO;
import com.vault.app.entity.SavedPassword;
import com.vault.app.entity.User;
import com.vault.app.repository.SavedPasswordRepository;
import com.vault.app.repository.UserRepository;
import com.vault.app.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BreachDetectionService {

    private final RestTemplate restTemplate;
    private final SavedPasswordRepository passwordRepository;
    private final UserRepository userRepository;
    private final EncryptionUtil encryptionUtil;

    private static final String HIBP_API = "https://api.pwnedpasswords.com/range";

    // 1. Checks a single password against the HIBP database
    public boolean isPasswordBreached(String plainTextPassword) {
        try {
            String sha1Hash = generateSHA1(plainTextPassword);
            String prefix = sha1Hash.substring(0, 5);
            String suffix = sha1Hash.substring(5);

            // Call HIBP API with ONLY the 5-character prefix
            String response = restTemplate.getForObject(HIBP_API + "/" + prefix, String.class);

            // Check if our suffix exists anywhere in the returned breach list
            return response != null && response.contains(suffix);

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return false;
            }
            throw new RuntimeException("Error calling breach API", e);
        } catch (Exception e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    // 2. Scans every password in a user's vault
    public BreachScanResultDTO scanAllPasswords(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<SavedPassword> vault = passwordRepository.findByUserId(user.getId());
        List<BreachedPasswordDTO> breachedList = new ArrayList<>();

        for (SavedPassword entry : vault) {
            String decryptedPassword = encryptionUtil.decrypt(entry.getEncryptedPassword());

            if (isPasswordBreached(decryptedPassword)) {
                entry.setIsBreached(true);
                breachedList.add(new BreachedPasswordDTO(
                        entry.getId(),
                        entry.getPlatform(),
                        entry.getLoginUsername(),
                        "Password found in public data breach. Change immediately!"
                ));
            } else {
                entry.setIsBreached(false);
            }
            passwordRepository.save(entry); // Update the breach status in the DB
        }

        return new BreachScanResultDTO(
                vault.size(),
                breachedList.size(),
                breachedList,
                LocalDateTime.now()
        );
    }

    // SHA-1 Hashing Algorithm
    private String generateSHA1(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        return String.format("%040x", new BigInteger(1, hash)).toUpperCase();
    }
}