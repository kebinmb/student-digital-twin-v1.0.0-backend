package com.sdt.web_app.service.webhook;

import tools.jackson.databind.ObjectMapper;
import com.sdt.web_app.entities.webhook.InstitutionalWebhook;
import com.sdt.web_app.entities.webhook.InstitutionalWebhookDelivery;
import com.sdt.web_app.repositories.webhook.InstitutionalWebhookDeliveryRepository;
import com.sdt.web_app.repositories.webhook.InstitutionalWebhookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstitutionalWebhookServiceTest {

    @Mock
    private InstitutionalWebhookRepository webhookRepository;

    @Mock
    private InstitutionalWebhookDeliveryRepository deliveryRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private InstitutionalWebhookService webhookService;

    private InstitutionalWebhook webhook;

    @BeforeEach
    void setUp() {
        webhook = InstitutionalWebhook.builder()
                .id(1L)
                .name("CHMSU External SIS")
                .targetUrl("https://sis.chmsu.edu.ph/webhook")
                .secretKey("whsec_secret123")
                .subscribedEvents("STUDENT_HONOR_AWARDED,TUITION_DISCOUNT_APPLIED")
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Compute HMAC SHA-256 generates consistent cryptographic signature")
    void computeHmacSha256_Valid() {
        String data = "{\"event\":\"STUDENT_HONOR_AWARDED\",\"studentId\":100}";
        String secret = "test-secret-key";

        String sig1 = InstitutionalWebhookService.computeHmacSha256(data, secret);
        String sig2 = InstitutionalWebhookService.computeHmacSha256(data, secret);

        assertThat(sig1).isNotBlank();
        assertThat(sig1).isEqualTo(sig2);
        assertThat(sig1.length()).isEqualTo(64); // SHA-256 hex length
    }

    @Test
    @DisplayName("Dispatch event serializes payload and creates delivery records for subscribed webhooks")
    void dispatchEvent_CreatesDeliveryRecords() throws Exception {
        given(webhookRepository.findByActiveTrue()).willReturn(List.of(webhook));
        given(objectMapper.writeValueAsString(any())).willReturn("{\"mock\":\"payload\"}");

        given(deliveryRepository.save(any(InstitutionalWebhookDelivery.class)))
                .willAnswer(inv -> {
                    InstitutionalWebhookDelivery d = inv.getArgument(0);
                    d.setId(10L);
                    return d;
                });

        webhookService.dispatchEvent("STUDENT_HONOR_AWARDED", Map.of("key", "value"));

        verify(deliveryRepository, atLeastOnce()).save(any(InstitutionalWebhookDelivery.class));
    }

    @Test
    @DisplayName("Dispatch event skips webhooks not subscribed to the event type")
    void dispatchEvent_SkipsUnsubscribedWebhooks() {
        given(webhookRepository.findByActiveTrue()).willReturn(List.of(webhook));

        webhookService.dispatchEvent("UNKNOWN_EVENT_TYPE", Map.of("key", "value"));

        verify(deliveryRepository, never()).save(any(InstitutionalWebhookDelivery.class));
    }

    @Test
    @DisplayName("Test ping creates signed PING_VERIFICATION delivery record")
    void testPing_CreatesPingDelivery() throws Exception {
        given(webhookRepository.findById(1L)).willReturn(Optional.of(webhook));
        given(objectMapper.writeValueAsString(any())).willReturn("{\"event\":\"PING_VERIFICATION\"}");
        given(deliveryRepository.save(any(InstitutionalWebhookDelivery.class)))
                .willAnswer(inv -> {
                    InstitutionalWebhookDelivery d = inv.getArgument(0);
                    d.setId(99L);
                    return d;
                });

        InstitutionalWebhookDelivery result = webhookService.testPing(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(99L);
        assertThat(result.getEventType()).isEqualTo("PING_VERIFICATION");
        verify(deliveryRepository, atLeastOnce()).save(any(InstitutionalWebhookDelivery.class));
    }
}
