package com.sdt.web_app.service.push;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.AlgorithmParameters;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECPoint;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebPushPayloadEncryptionServiceTest {

    private final WebPushPayloadEncryptionService encryptionService = new WebPushPayloadEncryptionService();

    @Test
    @DisplayName("Encrypt RFC 8291 payload produces valid aes128gcm binary body")
    void encrypt_ValidClientKeys_ProducesEncryptedRecord() throws Exception {
        // Generate valid client EC P-256 key
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair clientPair = kpg.generateKeyPair();
        ECPublicKey clientPubKey = (ECPublicKey) clientPair.getPublic();

        ECPoint point = clientPubKey.getW();
        byte[] x = toFixedLength(point.getAffineX().toByteArray(), 32);
        byte[] y = toFixedLength(point.getAffineY().toByteArray(), 32);
        byte[] uncompressed = new byte[65];
        uncompressed[0] = 0x04;
        System.arraycopy(x, 0, uncompressed, 1, 32);
        System.arraycopy(y, 0, uncompressed, 33, 32);

        String p256dh = Base64.getUrlEncoder().withoutPadding().encodeToString(uncompressed);
        String auth = Base64.getUrlEncoder().withoutPadding().encodeToString("16ByteAuthSecret!".getBytes());

        String jsonPayload = "{\"title\":\"Test Alert\",\"body\":\"Classroom room change.\"}";

        WebPushPayloadEncryptionService.EncryptedPushPayload result = encryptionService.encrypt(jsonPayload, p256dh, auth);

        assertThat(result).isNotNull();
        assertThat(result.contentEncoding()).isEqualTo("aes128gcm");
        assertThat(result.contentType()).isEqualTo("application/octet-stream");
        assertThat(result.body()).isNotNull();
        // RFC 8188 minimum header length: 16 (salt) + 4 (rs) + 1 (idlen) + 65 (key) + ciphertext (>16) = >102 bytes
        assertThat(result.body().length).isGreaterThan(100);
    }

    @Test
    @DisplayName("Encrypt with invalid key throws IllegalArgumentException")
    void encrypt_InvalidKeys_ThrowsException() {
        assertThatThrownBy(() -> encryptionService.encrypt("test", "short-invalid-key", "short-auth"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private byte[] toFixedLength(byte[] val, int length) {
        if (val.length == length) return val;
        byte[] fixed = new byte[length];
        if (val.length > length) {
            System.arraycopy(val, val.length - length, fixed, 0, length);
        } else {
            System.arraycopy(val, 0, fixed, length - val.length, val.length);
        }
        return fixed;
    }
}
