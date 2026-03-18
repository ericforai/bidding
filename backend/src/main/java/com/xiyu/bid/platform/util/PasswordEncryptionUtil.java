package com.xiyu.bid.platform.util;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Password Encryption Utility
 * Provides AES-256-GCM encryption for password storage
 */
@Component
@Slf4j
public class PasswordEncryptionUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 256;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private byte[] encryptionKey;

    @Value("${platform.account.encryption-key:}")
    private String configuredKey;

    @PostConstruct
    public void initialize() {
        String keyFromEnv = configuredKey != null && !configuredKey.trim().isEmpty()
                ? configuredKey
                : System.getenv("PLATFORM_ACCOUNT_ENCRYPTION_KEY");
        if (keyFromEnv == null || keyFromEnv.trim().isEmpty()) {
            String errorMsg = "PLATFORM_ACCOUNT_ENCRYPTION_KEY environment variable is required for password encryption. " +
                           "This is a security requirement - hardcoded keys are not allowed.";
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        // Validate minimum key length for security
        if (keyFromEnv.length() < 16) {
            String errorMsg = "PLATFORM_ACCOUNT_ENCRYPTION_KEY must be at least 16 characters for secure encryption. " +
                           "Current length: " + keyFromEnv.length();
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        // Ensure key is exactly 32 bytes (256 bits) for AES-256
        this.encryptionKey = deriveKey(keyFromEnv);
        log.info("PasswordEncryptionUtil initialized with AES-256-GCM");
    }

    /**
     * Encrypt a plain text password
     * @param plainPassword the plain text password to encrypt
     * @return Base64 encoded encrypted password, or null if input is null
     */
    public String encrypt(String plainPassword) {
        if (plainPassword == null) {
            return null;
        }

        try {
            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            // Initialize cipher for encryption
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(encryptionKey, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            // Encrypt the password
            byte[] encryptedData = cipher.doFinal(plainPassword.getBytes(StandardCharsets.UTF_8));

            // Combine IV and encrypted data
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encryptedData.length);
            byteBuffer.put(iv);
            byteBuffer.put(encryptedData);

            // Return Base64 encoded result
            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            log.error("Failed to encrypt password", e);
            throw new RuntimeException("Password encryption failed", e);
        }
    }

    /**
     * Decrypt an encrypted password
     * @param encryptedPassword the Base64 encoded encrypted password
     * @return the decrypted plain text password, or null if input is null
     * @throws RuntimeException if decryption fails
     */
    public String decrypt(String encryptedPassword) {
        if (encryptedPassword == null) {
            return null;
        }

        try {
            // Decode Base64
            byte[] decodedData = Base64.getDecoder().decode(encryptedPassword);

            // Extract IV and encrypted data
            ByteBuffer byteBuffer = ByteBuffer.wrap(decodedData);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] encryptedData = new byte[byteBuffer.remaining()];
            byteBuffer.get(encryptedData);

            // Initialize cipher for decryption
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(encryptionKey, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            // Decrypt the data
            byte[] decryptedData = cipher.doFinal(encryptedData);

            return new String(decryptedData, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to decrypt password", e);
            throw new RuntimeException("Password decryption failed", e);
        }
    }

    /**
     * Derive a 256-bit key from the provided key string using SHA-256
     * @param keyString the key string
     * @return a 32-byte array suitable for AES-256
     */
    private byte[] deriveKey(String keyString) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(keyString.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Validate the encryption key
     * @return true if the key is properly initialized
     */
    public boolean isKeyValid() {
        return encryptionKey != null && encryptionKey.length == 32;
    }
}
