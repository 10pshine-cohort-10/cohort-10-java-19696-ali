package com.shine.book;

import java.security.*;
import java.util.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.stereotype.Component;

@Component
class PasswordHash {
    private static final int ITERATIONS = 600_000;
    private final SecureRandom random = new SecureRandom();

    String encode(String password) {
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt) + "." +
                Base64.getEncoder().encodeToString(derive(password, salt));
    }

    boolean matches(String password, String stored) {
        String[] parts = stored.split("\\.", 2);
        return MessageDigest.isEqual(Base64.getDecoder().decode(parts[1]),
                derive(password, Base64.getDecoder().decode(parts[0])));
    }

    private byte[] derive(String password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Password hashing unavailable", ex);
        } finally {
            spec.clearPassword();
        }
    }
}
