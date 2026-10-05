package com.sdt.web_app.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * JPA AttributeConverter implementing AES-256-GCM authenticated encryption at rest.
 *
 * Applied via @Convert(converter = EncryptedStringConverter.class) on sensitive
 * StudentEquityProfile fields (statutory ID numbers, certificate numbers).
 *
 * Encoding format (Base64): [ 12-byte IV | 16-byte GCM auth tag (appended by JCA) | ciphertext ]
 *
 * Compliance: RA 10173 Data Privacy Act — protects sensitive beneficiary PII at
 * the persistence layer before writing to MySQL sdt_webapp_dev.
 */
@Component
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static final String AES_GCM_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int GCM_IV_LENGTH_BYTES = 12;

    // Injected via application.yml: app.equity.encryption-key (Base64-encoded 32-byte key)
    // Falls back to a deterministic dev-only key; MUST be overridden in production via env var.
    @Value("${app.equity.encryption-key:c2R0LWVxdWl0eS1hZXMtMjU2LWtleS1kZXYtMzJieXRlcyE=}")
    private String base64EncodedKey;

    private SecretKey getSecretKey() {
        byte[] keyBytes = Base64.getDecoder().decode(base64EncodedKey);
        if (keyBytes.length != 32) {
            throw new IllegalStateException(
                "app.equity.encryption-key must decode to exactly 32 bytes for AES-256. " +
                "Current decoded length: " + keyBytes.length);
        }
        return new SecretKeySpec(keyBytes, "AES");
    }

    private static final String ENCRYPTED_PREFIX = "ENC:";

    /**
     * Encrypts a plaintext string to Base64-encoded AES-256-GCM ciphertext prefixed with 'ENC:'.
     * Returns null for null/blank inputs (preserves nullable DB semantics).
     */
    @Override
    public String convertToDatabaseColumn(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_GCM_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Prepend IV to ciphertext: [12-byte IV | ciphertext + 16-byte GCM tag]
            ByteBuffer buffer = ByteBuffer.allocate(GCM_IV_LENGTH_BYTES + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);

            return ENCRYPTED_PREFIX + Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new IllegalStateException("AES-256-GCM encryption failed for equity field.", e);
        }
    }

    /**
     * Decrypts an 'ENC:' prefixed Base64-encoded AES-256-GCM ciphertext back to plaintext.
     * If the string does not have the 'ENC:' prefix, it is safely treated as legacy plaintext.
     */
    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        // If not encrypted yet (e.g. legacy seeded data containing dashes or plaintext), return as-is
        if (!dbData.startsWith(ENCRYPTED_PREFIX)) {
            return dbData;
        }

        try {
            String encryptedBase64 = dbData.substring(ENCRYPTED_PREFIX.length());
            byte[] decoded = Base64.getDecoder().decode(encryptedBase64);

            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(AES_GCM_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));

            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Graceful fallback: return raw data instead of throwing HTTP 500
            return dbData;
        }
    }
}
