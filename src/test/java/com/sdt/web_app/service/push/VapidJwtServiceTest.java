package com.sdt.web_app.service.push;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class VapidJwtServiceTest {

    private VapidJwtService vapidJwtService;

    @BeforeEach
    void setUp() {
        vapidJwtService = new VapidJwtService();
        vapidJwtService.init();
    }

    @Test
    @DisplayName("VapidJwtService initializes valid uncompressed public key")
    void init_CreatesValidPublicKey() {
        String pubKey = vapidJwtService.getPublicKeyBase64Url();
        assertThat(pubKey).isNotBlank();
        byte[] rawKey = Base64.getUrlDecoder().decode(pubKey);
        assertThat(rawKey).hasSize(65);
        assertThat(rawKey[0]).isEqualTo((byte) 0x04); // Uncompressed point indicator
    }

    @Test
    @DisplayName("Create VAPID authorization header returns compliant RFC 8292 token")
    void createAuthorizationHeader_ValidEndpoint_ReturnsVapidHeader() {
        String endpoint = "https://fcm.googleapis.com/fcm/send/device-xyz-123";
        String header = vapidJwtService.createAuthorizationHeader(endpoint);

        assertThat(header).isNotNull();
        assertThat(header).startsWith("vapid t=");
        assertThat(header).contains(", k=");

        // Extract JWT token part
        String token = header.substring("vapid t=".length(), header.indexOf(", k="));
        String[] parts = token.split("\\.");
        assertThat(parts).hasSize(3);

        // Verify header claims
        String decodedHeader = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        assertThat(decodedHeader).contains("\"alg\":\"ES256\"");

        // Verify payload claims
        String decodedPayload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        assertThat(decodedPayload).contains("\"aud\":\"https://fcm.googleapis.com\"");
        assertThat(decodedPayload).contains("\"sub\":\"mailto:registrar@chmsu.edu.ph\"");
        assertThat(decodedPayload).contains("\"exp\":");

        // Verify IEEE P1363 signature length (64 bytes -> 86 Base64URL characters without padding)
        byte[] signatureBytes = Base64.getUrlDecoder().decode(parts[2]);
        assertThat(signatureBytes).hasSize(64);
    }

    @Test
    @DisplayName("VapidJwtService loads configured persistent keypair correctly")
    void init_WithConfiguredKeyPair_LoadsSuccessfully() throws Exception {
        // Generate a real keypair to extract encoded forms
        java.security.KeyPairGenerator kpg = java.security.KeyPairGenerator.getInstance("EC");
        kpg.initialize(new java.security.spec.ECGenParameterSpec("secp256r1"));
        java.security.KeyPair kp = kpg.generateKeyPair();

        String privBase64 = Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded());
        String pubBase64 = Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());

        VapidJwtService customService = new VapidJwtService();
        org.springframework.test.util.ReflectionTestUtils.setField(customService, "configuredPrivateKeyBase64", privBase64);
        org.springframework.test.util.ReflectionTestUtils.setField(customService, "configuredPublicKeyBase64", pubBase64);
        customService.init();

        assertThat(customService.getPublicKeyBase64Url()).isNotBlank();
        String header = customService.createAuthorizationHeader("https://fcm.googleapis.com/fcm/send/test-sub");
        assertThat(header).isNotNull();
        assertThat(header).contains(", k=" + customService.getPublicKeyBase64Url());
    }
}
