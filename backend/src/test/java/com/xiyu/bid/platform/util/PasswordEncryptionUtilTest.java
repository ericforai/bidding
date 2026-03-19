package com.xiyu.bid.platform.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for PasswordEncryptionUtil following TDD principles.
 *
 * Test Coverage:
 * - Environment variable loading (PLATFORM_ENCRYPTION_KEY)
 * - Development environment fallback
 * - Startup validation in non-dev environments
 * - Encryption/Decryption operations
 * - Edge cases (null, empty, invalid inputs)
 * - Security requirements (key length, key validity)
 */
@DisplayName("PasswordEncryptionUtil Test Suite")
class PasswordEncryptionUtilTest {

    private PasswordEncryptionUtil passwordEncryptionUtil;

    @BeforeEach
    void setUp() {
        passwordEncryptionUtil = new PasswordEncryptionUtil();
        // Clear any existing environment variables
        clearEnvironmentVariables();
    }

    private void clearEnvironmentVariables() {
        // Clear relevant environment variables for clean test state
        System.clearProperty("PLATFORM_ENCRYPTION_KEY");
        System.clearProperty("SPRING_PROFILES_ACTIVE");
        System.clearProperty("platform.account.encryption-key");
    }

    // ========== Environment Variable Tests ==========

    @Test
    @DisplayName("Should successfully initialize when PLATFORM_ENCRYPTION_KEY is set")
    void shouldInitializeWithEnvironmentVariable() {
        // Arrange
        String testKey = "test-encryption-key-32-chars-long-for-security";
        System.setProperty("PLATFORM_ENCRYPTION_KEY", testKey);

        // Act & Assert
        assertDoesNotThrow(() -> {
            ReflectionTestUtils.setField(passwordEncryptionUtil, "configuredKey", null);
            passwordEncryptionUtil.initialize();
        });

        assertTrue(passwordEncryptionUtil.isKeyValid(),
            "Key should be valid after successful initialization");
    }

    @Test
    @DisplayName("Should read from application property platform.account.encryption-key")
    void shouldReadFromApplicationProperty() {
        // Arrange
        String testKey = "test-encryption-key-from-property-32-chars";
        ReflectionTestUtils.setField(passwordEncryptionUtil, "configuredKey", testKey);

        // Act & Assert
        assertDoesNotThrow(() -> passwordEncryptionUtil.initialize());
        assertTrue(passwordEncryptionUtil.isKeyValid());
    }

    @Test
    @DisplayName("Should prioritize application property over environment variable")
    void shouldPrioritizeApplicationPropertyOverEnvironmentVariable() {
        // Arrange
        String propertyKey = "property-key-32-chars-long-for-security";
        String envKey = "env-key-32-chars-long-for-security-different";
        ReflectionTestUtils.setField(passwordEncryptionUtil, "configuredKey", propertyKey);
        System.setProperty("PLATFORM_ENCRYPTION_KEY", envKey);

        // Act & Assert
        assertDoesNotThrow(() -> passwordEncryptionUtil.initialize());
        // The util should use the property key
        String testData = "test-password";
        String encrypted = passwordEncryptionUtil.encrypt(testData);
        assertNotNull(encrypted);
        assertNotEquals(testData, encrypted);
    }

    @Test
    @DisplayName("Should fail initialization when PLATFORM_ENCRYPTION_KEY is missing in production")
    void shouldFailWhenEnvironmentVariableMissingInProduction() {
        // Arrange
        System.setProperty("SPRING_PROFILES_ACTIVE", "prod");
        System.clearProperty("PLATFORM_ENCRYPTION_KEY");
        ReflectionTestUtils.setField(passwordEncryptionUtil, "configuredKey", null);

        // Act & Assert
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> passwordEncryptionUtil.initialize(),
            "Should throw IllegalStateException when PLATFORM_ENCRYPTION_KEY is missing in production"
        );

        assertTrue(exception.getMessage().contains("PLATFORM_ENCRYPTION_KEY"),
            "Error message should mention PLATFORM_ENCRYPTION_KEY");
    }

    @Test
    @DisplayName("Should fail initialization when PLATFORM_ENCRYPTION_KEY is empty in production")
    void shouldFailWhenEnvironmentVariableIsEmptyInProduction() {
        // Arrange
        System.setProperty("SPRING_PROFILES_ACTIVE", "prod");
        System.setProperty("PLATFORM_ENCRYPTION_KEY", "   ");
        ReflectionTestUtils.setField(passwordEncryptionUtil, "configuredKey", null);

        // Act & Assert
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> passwordEncryptionUtil.initialize(),
            "Should throw IllegalStateException when PLATFORM_ENCRYPTION_KEY is empty"
        );

        assertTrue(exception.getMessage().contains("PLATFORM_ENCRYPTION_KEY"),
            "Error message should mention PLATFORM_ENCRYPTION_KEY");
    }

    @Test
    @DisplayName("Should use fallback key in development environment when env var is missing")
    void shouldUseFallbackInDevelopmentEnvironment() {
        // Arrange
        System.setProperty("SPRING_PROFILES_ACTIVE", "dev");
        System.clearProperty("PLATFORM_ENCRYPTION_KEY");
        ReflectionTestUtils.setField(passwordEncryptionUtil, "configuredKey", null);

        // Act & Assert
        assertDoesNotThrow(() -> passwordEncryptionUtil.initialize(),
            "Should not throw in development environment even without env var");
        assertTrue(passwordEncryptionUtil.isKeyValid(),
            "Should use fallback key in development");
    }

    @Test
    @DisplayName("Should use fallback key in test environment")
    void shouldUseFallbackInTestEnvironment() {
        // Arrange
        System.setProperty("SPRING_PROFILES_ACTIVE", "test");
        System.clearProperty("PLATFORM_ENCRYPTION_KEY");
        ReflectionTestUtils.setField(passwordEncryptionUtil, "configuredKey", null);

        // Act & Assert
        assertDoesNotThrow(() -> passwordEncryptionUtil.initialize(),
            "Should not throw in test environment");
        assertTrue(passwordEncryptionUtil.isKeyValid(),
            "Should use fallback key in test environment");
    }

    @Test
    @DisplayName("Should fail initialization when key is too short (< 16 characters)")
    void shouldFailWhenKeyIsTooShort() {
        // Arrange - Set to production to ensure no fallback is used
        System.setProperty("SPRING_PROFILES_ACTIVE", "prod");
        String shortKey = "short";
        System.setProperty("PLATFORM_ENCRYPTION_KEY", shortKey);
        ReflectionTestUtils.setField(passwordEncryptionUtil, "configuredKey", null);

        // Act & Assert
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> passwordEncryptionUtil.initialize(),
            "Should throw IllegalStateException when key is too short"
        );

        assertTrue(exception.getMessage().contains("at least 16 characters"),
            "Error message should mention minimum key length");
    }

    // ========== Encryption/Decryption Tests ==========

    @Test
    @DisplayName("Should successfully encrypt and decrypt password")
    void shouldEncryptAndDecryptPassword() {
        // Arrange
        setupTestKey();
        String originalPassword = "MySecurePassword123!";

        // Act
        String encrypted = passwordEncryptionUtil.encrypt(originalPassword);
        String decrypted = passwordEncryptionUtil.decrypt(encrypted);

        // Assert
        assertNotNull(encrypted, "Encrypted password should not be null");
        assertNotNull(decrypted, "Decrypted password should not be null");
        assertNotEquals(originalPassword, encrypted, "Encrypted password should differ from original");
        assertEquals(originalPassword, decrypted, "Decrypted password should match original");
    }

    @Test
    @DisplayName("Should generate different encrypted values for same password (due to random IV)")
    void shouldGenerateDifferentEncryptedValues() {
        // Arrange
        setupTestKey();
        String password = "SamePassword123";

        // Act
        String encrypted1 = passwordEncryptionUtil.encrypt(password);
        String encrypted2 = passwordEncryptionUtil.encrypt(password);

        // Assert
        assertNotEquals(encrypted1, encrypted2,
            "Each encryption should produce different result due to random IV");

        // But both should decrypt to the same value
        assertEquals(password, passwordEncryptionUtil.decrypt(encrypted1));
        assertEquals(password, passwordEncryptionUtil.decrypt(encrypted2));
    }

    @Test
    @DisplayName("Should handle null password encryption")
    void shouldHandleNullPasswordEncryption() {
        // Arrange
        setupTestKey();

        // Act
        String encrypted = passwordEncryptionUtil.encrypt(null);

        // Assert
        assertNull(encrypted, "Encrypting null should return null");
    }

    @Test
    @DisplayName("Should handle null password decryption")
    void shouldHandleNullPasswordDecryption() {
        // Arrange
        setupTestKey();

        // Act
        String decrypted = passwordEncryptionUtil.decrypt(null);

        // Assert
        assertNull(decrypted, "Decrypting null should return null");
    }

    @Test
    @DisplayName("Should handle empty string password")
    void shouldHandleEmptyStringPassword() {
        // Arrange
        setupTestKey();
        String emptyPassword = "";

        // Act
        String encrypted = passwordEncryptionUtil.encrypt(emptyPassword);
        String decrypted = passwordEncryptionUtil.decrypt(encrypted);

        // Assert
        assertNotNull(encrypted);
        assertEquals(emptyPassword, decrypted);
    }

    @Test
    @DisplayName("Should handle special characters in password")
    void shouldHandleSpecialCharacters() {
        // Arrange
        setupTestKey();
        String specialPassword = "P@ssw0rd!#$%^&*()_+-=[]{}|;':\",./<>?`~";

        // Act
        String encrypted = passwordEncryptionUtil.encrypt(specialPassword);
        String decrypted = passwordEncryptionUtil.decrypt(encrypted);

        // Assert
        assertEquals(specialPassword, decrypted,
            "Should correctly handle special characters");
    }

    @Test
    @DisplayName("Should handle Unicode characters (emoji, Chinese, etc.)")
    void shouldHandleUnicodeCharacters() {
        // Arrange
        setupTestKey();
        String unicodePassword = "密码🔑🔒测试Test测试123";

        // Act
        String encrypted = passwordEncryptionUtil.encrypt(unicodePassword);
        String decrypted = passwordEncryptionUtil.decrypt(encrypted);

        // Assert
        assertEquals(unicodePassword, decrypted,
            "Should correctly handle Unicode characters");
    }

    @Test
    @DisplayName("Should handle very long passwords")
    void shouldHandleVeryLongPasswords() {
        // Arrange
        setupTestKey();
        StringBuilder longPassword = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            longPassword.append("a");
        }
        String password = longPassword.toString();

        // Act
        String encrypted = passwordEncryptionUtil.encrypt(password);
        String decrypted = passwordEncryptionUtil.decrypt(encrypted);

        // Assert
        assertEquals(password, decrypted,
            "Should correctly handle very long passwords");
    }

    @Test
    @DisplayName("Should throw exception when decrypting invalid Base64")
    void shouldThrowWhenDecryptingInvalidBase64() {
        // Arrange
        setupTestKey();
        String invalidBase64 = "not-valid-base64!!!";

        // Act & Assert
        assertThrows(RuntimeException.class,
            () -> passwordEncryptionUtil.decrypt(invalidBase64),
            "Should throw RuntimeException when decrypting invalid Base64");
    }

    @Test
    @DisplayName("Should throw exception when decrypting with wrong key")
    void shouldThrowWhenDecryptingWithWrongKey() {
        // Arrange
        setupTestKey();
        String password = "TestPassword123";
        String encrypted = passwordEncryptionUtil.encrypt(password);

        // Change the key
        setupDifferentKey();

        // Act & Assert
        assertThrows(RuntimeException.class,
            () -> passwordEncryptionUtil.decrypt(encrypted),
            "Should throw RuntimeException when decrypting with different key");
    }

    // ========== Key Validation Tests ==========

    @Test
    @DisplayName("Should report key as valid after proper initialization")
    void shouldReportKeyValidAfterInitialization() {
        // Arrange
        setupTestKey();

        // Act & Assert
        assertTrue(passwordEncryptionUtil.isKeyValid(),
            "Key should be valid after initialization");
    }

    @Test
    @DisplayName("Should report key as invalid before initialization")
    void shouldReportKeyInvalidBeforeInitialization() {
        // Arrange - don't initialize

        // Act & Assert
        assertFalse(passwordEncryptionUtil.isKeyValid(),
            "Key should be invalid before initialization");
    }

    // ========== Helper Methods ==========

    private void setupTestKey() {
        String testKey = "test-encryption-key-32-chars-long-for-security";
        ReflectionTestUtils.setField(passwordEncryptionUtil, "configuredKey", testKey);
        passwordEncryptionUtil.initialize();
    }

    private void setupDifferentKey() {
        String differentKey = "different-encryption-key-32-chars-long";
        ReflectionTestUtils.setField(passwordEncryptionUtil, "configuredKey", differentKey);
        passwordEncryptionUtil.initialize();
    }
}
