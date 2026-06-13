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

/**
 * Service responsible for external threat intelligence integrations.
 * Scans user credentials against known data breaches utilizing a
 * privacy-preserving k-Anonymity model.
 */
@Service
@RequiredArgsConstructor
public class BreachDetectionService {

    private final RestTemplate restTemplate;
    private final SavedPasswordRepository passwordRepository;
    private final UserRepository userRepository;
    private final EncryptionUtil encryptionUtil;

    private static final String HIBP_API = "https://api.pwnedpasswords.com/range";

    /**
     * Checks a single plaintext password against the HaveIBeenPwned database.
     * Employs k-Anonymity by only transmitting the first 5 characters of the SHA-1 hash.
     * The full password or full hash never leaves the local server.
     *
     * @param plainTextPassword The decrypted password to check
     * @return boolean True if found in a public data breach, false otherwise
     */
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

    /**
     * Orchestrates a full vault scan for a specific user.
     * Decrypts each stored password in memory, checks for breaches,
     * updates the database flags, and aggregates the results.
     *
     * @param username The authenticated user's username
     * @return BreachScanResultDTO containing the total scanned and any breached credentials
     */
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

    /**
     * Utility method to generate a SHA-1 hash.
     * Used exclusively for k-Anonymity API integration, NOT for password storage.
     */
    private String generateSHA1(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        return String.format("%040x", new BigInteger(1, hash)).toUpperCase();
    }
}