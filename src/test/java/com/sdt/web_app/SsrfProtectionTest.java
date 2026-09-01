package com.sdt.web_app;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class SsrfProtectionTest extends BaseIntegrationTest {
    @Autowired
    private RestClient safeRestClient;

    @ParameterizedTest
    @ValueSource(strings = {
            "http://169.254.169.254/latest/meta-data/",
            "http://localhost:8080/admin",
            "http://127.0.0.1:9090",
            "http://192.168.1.1/router"
    })
    @DisplayName("Outbound RestClient must throw SecurityException for restriced hosts/subnet")
    void blocksOutboundSsrfRequests(String targetUrl) {
        assertThrows(SecurityException.class, () -> {
            safeRestClient.get()
                    .uri(targetUrl)
                    .retrieve()
                    .toBodilessEntity();
        });
    }
}
