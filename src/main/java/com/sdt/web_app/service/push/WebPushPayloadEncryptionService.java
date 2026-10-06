package com.sdt.web_app.service.push;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.util.Arrays;
import java.util.Base64;

/**
 * Implements RFC 8291 (Message Encryption for Web Push) and RFC 8188 (Encrypted Content-Encoding).
 * Encrypts arbitrary UTF-8 payloads with AES-128-GCM using client ECDH P-256 public key and auth secret.
 */
@Service
@Slf4j
public class WebPushPayloadEncryptionService {

    private static final String CURVE_NAME = "secp256r1";
    private static final int RECORD_SIZE = 4096;

    public record EncryptedPushPayload(
            byte[] body,
            String contentEncoding,
            String contentType
    ) {}

    /**
     * Encrypts plaintext using RFC 8291 aes128gcm scheme.
     *
     * @param plaintext Plaintext string (usually JSON)
     * @param p256dhBase64 Base64 or Base64URL encoded client uncompressed P-256 public key
     * @param authBase64 Base64 or Base64URL encoded 16-byte client auth secret
     * @return Encrypted binary record with content-encoding aes128gcm
     */
    public EncryptedPushPayload encrypt(String plaintext, String p256dhBase64, String authBase64) throws Exception {
        if (plaintext == null) {
            plaintext = "";
        }
        byte[] clientPubKeyBytes = decodeBase64(p256dhBase64);
        byte[] authBytes = decodeBase64(authBase64);

        if (clientPubKeyBytes.length != 65 || clientPubKeyBytes[0] != 0x04) {
            throw new IllegalArgumentException("Invalid client P-256 public key length or format. Expected 65-byte uncompressed point.");
        }
        if (authBytes.length < 16) {
            throw new IllegalArgumentException("Invalid auth secret length. Expected at least 16 bytes.");
        }

        // 1. Generate Ephemeral EC KeyPair
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec(CURVE_NAME));
        KeyPair serverKeyPair = kpg.generateKeyPair();
        ECPublicKey serverPubKey = (ECPublicKey) serverKeyPair.getPublic();
        byte[] serverPubKeyBytes = encodeUncompressedPoint(serverPubKey);

        // 2. Decode Client Public Key
        ECPublicKey clientPubKey = decodeClientPublicKey(clientPubKeyBytes);

        // 3. Perform ECDH to get shared secret
        KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        ka.init(serverKeyPair.getPrivate());
        ka.doPhase(clientPubKey, true);
        byte[] ecdhSecret = ka.generateSecret();

        // 4. Derive pseudo-random key (PRK) using auth secret as salt (RFC 8291 Section 3.2)
        // IKM = HKDF-Extract(auth, ecdhSecret)
        // PRK_key = HKDF-Expand(IKM, "WebPush: info" || 0x00 || clientPubKey || serverPubKey, 32)
        byte[] ikm = hkdfExtract(authBytes, ecdhSecret);

        ByteArrayOutputStream keyInfo = new ByteArrayOutputStream();
        keyInfo.write("WebPush: info\0".getBytes(StandardCharsets.UTF_8));
        keyInfo.write(clientPubKeyBytes);
        keyInfo.write(serverPubKeyBytes);

        byte[] prkKey = hkdfExpand(ikm, keyInfo.toByteArray(), 32);

        // 5. Generate random 16-byte salt for record
        byte[] salt = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(salt);

        // 6. PRK = HKDF-Extract(salt, prkKey)
        byte[] prk = hkdfExtract(salt, prkKey);

        // 7. Derive Content Encryption Key (CEK) & Nonce (RFC 8188 Section 2.1 & 2.2)
        byte[] cek = hkdfExpand(prk, "Content-Encoding: aes128gcm\0".getBytes(StandardCharsets.UTF_8), 16);
        byte[] nonce = hkdfExpand(prk, "Content-Encoding: nonce\0".getBytes(StandardCharsets.UTF_8), 12);

        // 8. Plaintext padding with 0x02 delimiter (final record)
        byte[] plainBytes = plaintext.getBytes(StandardCharsets.UTF_8);
        byte[] padded = new byte[plainBytes.length + 1];
        System.arraycopy(plainBytes, 0, padded, 0, plainBytes.length);
        padded[plainBytes.length] = 0x02; // delimiter for final record

        // 9. AES-128-GCM Encrypt
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(cek, "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, nonce);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
        byte[] ciphertext = cipher.doFinal(padded);

        // 10. Assemble RFC 8188 header:
        // salt (16 bytes) | rs (4 bytes) | idlen (1 byte) | keyid (65 bytes) | ciphertext
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(salt);
        out.write(ByteBuffer.allocate(4).putInt(RECORD_SIZE).array());
        out.write(65); // idlen
        out.write(serverPubKeyBytes);
        out.write(ciphertext);

        return new EncryptedPushPayload(
                out.toByteArray(),
                "aes128gcm",
                "application/octet-stream"
        );
    }

    private byte[] decodeBase64(String value) {
        if (value == null) return new byte[0];
        try {
            return Base64.getUrlDecoder().decode(value);
        } catch (IllegalArgumentException e) {
            return Base64.getDecoder().decode(value);
        }
    }

    private byte[] encodeUncompressedPoint(ECPublicKey key) {
        ECPoint point = key.getW();
        byte[] x = toFixedLength(point.getAffineX().toByteArray(), 32);
        byte[] y = toFixedLength(point.getAffineY().toByteArray(), 32);
        byte[] result = new byte[65];
        result[0] = 0x04;
        System.arraycopy(x, 0, result, 1, 32);
        System.arraycopy(y, 0, result, 33, 32);
        return result;
    }

    private ECPublicKey decodeClientPublicKey(byte[] uncompressed) throws Exception {
        byte[] xBytes = Arrays.copyOfRange(uncompressed, 1, 33);
        byte[] yBytes = Arrays.copyOfRange(uncompressed, 33, 65);
        BigInteger x = new BigInteger(1, xBytes);
        BigInteger y = new BigInteger(1, yBytes);
        ECPoint point = new ECPoint(x, y);

        AlgorithmParameters params = AlgorithmParameters.getInstance("EC");
        params.init(new ECGenParameterSpec(CURVE_NAME));
        ECParameterSpec ecParams = params.getParameterSpec(ECParameterSpec.class);
        ECPublicKeySpec spec = new ECPublicKeySpec(point, ecParams);

        KeyFactory kf = KeyFactory.getInstance("EC");
        return (ECPublicKey) kf.generatePublic(spec);
    }

    private byte[] toFixedLength(byte[] val, int length) {
        if (val.length == length) return val;
        byte[] fixed = new byte[length];
        if (val.length > length) {
            // Trim leading zero sign byte
            System.arraycopy(val, val.length - length, fixed, 0, length);
        } else {
            // Pad leading zeros
            System.arraycopy(val, 0, fixed, length - val.length, val.length);
        }
        return fixed;
    }

    private byte[] hkdfExtract(byte[] salt, byte[] ikm) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec key = new SecretKeySpec(salt != null && salt.length > 0 ? salt : new byte[32], "HmacSHA256");
        mac.init(key);
        return mac.doFinal(ikm);
    }

    private byte[] hkdfExpand(byte[] prk, byte[] info, int length) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(prk, "HmacSHA256"));
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] t = new byte[0];
        int count = 1;
        while (result.size() < length) {
            mac.reset();
            mac.update(t);
            mac.update(info);
            mac.update((byte) count++);
            t = mac.doFinal();
            result.write(t);
        }
        byte[] expanded = result.toByteArray();
        return Arrays.copyOf(expanded, length);
    }
}
