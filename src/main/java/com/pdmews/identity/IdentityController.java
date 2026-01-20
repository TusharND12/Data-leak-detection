package com.pdmews.identity;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@RestController
@RequestMapping("/api/identity")
@RequiredArgsConstructor
public class IdentityController {

    private final UserRepository userRepository;
    private final IdentifierRepository identifierRepository;

    @PostMapping("/users")
    public ResponseEntity<User> createUser(@RequestBody CreateUserRequest request) {
        User user = User.builder()
                .username(request.username())
                .passwordHash("hashed_" + request.password()) // Simple mock hash
                .build();
        return ResponseEntity.ok(userRepository.save(user));
    }

    @PostMapping("/identifiers")
    public ResponseEntity<Identifier> createIdentifier(@RequestBody CreateIdentifierRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String hash = hashIdentifier(request.identifier());

        Identifier identifier = Identifier.builder()
                .user(user)
                .type(request.type())
                .label(request.label())
                .identifierHash(hash)
                .build();

        return ResponseEntity.ok(identifierRepository.save(identifier));
    }

    private String hashIdentifier(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public record CreateUserRequest(String username, String password) {
    }

    public record CreateIdentifierRequest(UUID userId, Identifier.ContactType type, String identifier,
            String label) {
    }
}
