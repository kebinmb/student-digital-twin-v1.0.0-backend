package com.sdt.web_app.service.push;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECPoint;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;

/**
 * Implements RFC 8292 (Voluntary Application Server Identification - VAPID for Web Push).
 * Signs ES256 JSON Web Tokens (JWT) for push gateways (FCM, Mozilla, Apple).
 */
@Service
@Slf4j
public class VapidJwtService {

    private static final String CURVE_NAME = "secp256r1";

    @Value("${sdt.push.vapid.subject:mailto:registrar@chmsu.edu.ph}")
    private String subject = "mailto:registrar@chmsu.edu.ph";

    @Value("${sdt.push.vapid.private-key-base64:}")
    private String configuredPrivateKeyBase64 = "";

    @Value("${sdt.push.vapid.public-key-base64:}")
    private String configuredPublicKeyBase64 = "";

    private KeyPair vapidKeyPair;
    private String publicKeyBase64Url;

    @PostConstruct
    public void init() {
        try {
            if (configuredPrivateKeyBase64 != null && !configuredPrivateKeyBase64.isBlank()
                    && configuredPublicKeyBase64 != null && !configuredPublicKeyBase64.isBlank()) {
                loadConfiguredKeyPair();
                log.info("Initialized persistent VAPID EC keypair from configuration. Public Key: {}", this.publicKeyBase64Url);
                return;
            }

            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
            kpg.initialize(new ECGenParameterSpec(CURVE_NAME));
            this.vapidKeyPair = kpg.generateKeyPair();

            ECPublicKey pubKey = (ECPublicKey) this.vapidKeyPair.getPublic();
            byte[] rawUncompressed = encodeUncompressedPoint(pubKey);
            this.publicKeyBase64Url = Base64.getUrlEncoder().withoutPadding().encodeToString(rawUncompressed);
            log.info("Initialized ephemeral/active VAPID EC keypair. Public Key: {}", this.publicKeyBase64Url);
        } catch (Exception e) {
            log.error("Failed to initialize VAPID keypair", e);
        }
    }

    private void loadConfiguredKeyPair() throws Exception {
        KeyFactory kf = KeyFactory.getInstance("EC");
        byte[] privBytes = Base64.getDecoder().decode(configuredPrivateKeyBase64.trim());
        java.security.spec.PKCS8EncodedKeySpec privSpec = new java.security.spec.PKCS8EncodedKeySpec(privBytes);
        PrivateKey privKey = kf.generatePrivate(privSpec);

        byte[] pubBytes = Base64.getDecoder().decode(configuredPublicKeyBase64.trim());
        PublicKey pubKey;
        try {
            java.security.spec.X509EncodedKeySpec pubSpec = new java.security.spec.X509EncodedKeySpec(pubBytes);
            pubKey = kf.generatePublic(pubSpec);
        } catch (Exception x509Ex) {
            // Alternatively if raw uncompressed point (65 bytes 0x04...) was provided
            pubKey = decodeUncompressedPoint(pubBytes);
        }

        this.vapidKeyPair = new KeyPair(pubKey, privKey);
        byte[] rawUncompressed = encodeUncompressedPoint((ECPublicKey) pubKey);
        this.publicKeyBase64Url = Base64.getUrlEncoder().withoutPadding().encodeToString(rawUncompressed);
    }

    private ECPublicKey decodeUncompressedPoint(byte[] uncompressed) throws Exception {
        if (uncompressed.length != 65 || uncompressed[0] != 0x04) {
            throw new IllegalArgumentException("Invalid uncompressed EC point format");
        }
        byte[] xBytes = Arrays.copyOfRange(uncompressed, 1, 33);
        byte[] yBytes = Arrays.copyOfRange(uncompressed, 33, 65);
        java.math.BigInteger x = new java.math.BigInteger(1, xBytes);
        java.math.BigInteger y = new java.math.BigInteger(1, yBytes);
        ECPoint point = new ECPoint(x, y);

        AlgorithmParameters params = AlgorithmParameters.getInstance("EC");
        params.init(new ECGenParameterSpec(CURVE_NAME));
        java.security.spec.ECParameterSpec ecSpec = params.getParameterSpec(java.security.spec.ECParameterSpec.class);
        java.security.spec.ECPublicKeySpec pubSpec = new java.security.spec.ECPublicKeySpec(point, ecSpec);
        KeyFactory kf = KeyFactory.getInstance("EC");
        return (ECPublicKey) kf.generatePublic(pubSpec);
    }

    public String getPublicKeyBase64Url() {
        return publicKeyBase64Url;
    }

    /**
     * Builds and signs an RFC 8292 VAPID Authorization header string for the given push service endpoint.
     *
     * @param endpointUri Target push endpoint (e.g. https://fcm.googleapis.com/fcm/send/...)
     * @return Formatted Authorization header value (e.g. "vapid t=..., k=...")
     */
    public String createAuthorizationHeader(String endpointUri) {
        if (endpointUri == null || vapidKeyPair == null) {
            return null;
        }

        try {
            URI uri = URI.create(endpointUri);
            String audience = uri.getScheme() + "://" + uri.getHost() + (uri.getPort() > 0 ? ":" + uri.getPort() : "");

            long exp = Instant.now().plusSeconds(12 * 3600).getEpochSecond(); // 12 hours validity

            String headerJson = "{\"typ\":\"JWT\",\"alg\":\"ES256\"}";
            String payloadJson = String.format("{\"aud\":\"%s\",\"exp\":%d,\"sub\":\"%s\"}", audience, exp, subject);

            String encodedHeader = Base64.getUrlEncoder().withoutPadding().encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
            String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
            String signInput = encodedHeader + "." + encodedPayload;

            byte[] signatureBytes = signEs256(signInput.getBytes(StandardCharsets.UTF_8), (ECPrivateKey) vapidKeyPair.getPrivate());
            String encodedSignature = Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);

            String token = signInput + "." + encodedSignature;
            return "vapid t=" + token + ", k=" + publicKeyBase64Url;
        } catch (Exception e) {
            log.warn("Failed to generate VAPID JWT for endpoint {}: {}", endpointUri, e.getMessage());
            return null;
        }
    }

    private byte[] signEs256(byte[] data, ECPrivateKey privateKey) throws Exception {
        Signature signature;
        try {
            // Java 9+ IEEE P1363 format produces exactly 64 bytes (32-byte r || 32-byte s)
            signature = Signature.getInstance("SHA256withECDSAinP1363Format");
            signature.initSign(privateKey);
            signature.update(data);
            return signature.sign();
        } catch (NoSuchAlgorithmException e) {
            // Fallback to standard DER signature and convert to 64-byte P1363 format
            signature = Signature.getInstance("SHA256withECDSA");
            signature.initSign(privateKey);
            signature.update(data);
            byte[] der = signature.sign();
            return derToJose(der);
        }
    }

    private byte[] derToJose(byte[] der) {
        // DER sequence: 0x30, seqLen, 0x02, rLen, r..., 0x02, sLen, s...
        int rIndex = 2;
        int rLen = der[rIndex + 1];
        int rDataStart = rIndex + 2;

        int sIndex = rDataStart + rLen;
        int sLen = der[sIndex + 1];
        int sDataStart = sIndex + 2;

        // Skip leading zero byte if present (due to ASN.1 sign bit)
        if (der[rDataStart] == 0 && rLen > 32) {
            rDataStart++;
            rLen--;
        }
        if (der[sDataStart] == 0 && sLen > 32) {
            sDataStart++;
            sLen--;
        }

        byte[] jose = new byte[64];
        System.arraycopy(der, rDataStart, jose, 32 - rLen, rLen);
        System.arraycopy(der, sDataStart, jose, 64 - sLen, sLen);
        return jose;
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
