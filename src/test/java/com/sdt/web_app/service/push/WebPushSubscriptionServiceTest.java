package com.sdt.web_app.service.push;

import com.sdt.web_app.dto.push.PushDtos.*;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.push.WebPushSubscription;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.repositories.push.WebPushSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WebPushSubscriptionServiceTest {

    @Mock
    private WebPushSubscriptionRepository subscriptionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WebPushPayloadEncryptionService encryptionService;

    @Mock
    private VapidJwtService vapidJwtService;

    @InjectMocks
    private WebPushSubscriptionService webPushService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("2026-0001")
                .build();
    }

    @Test
    @DisplayName("Register new web push subscription creates active record")
    void registerSubscription_New_Success() {
        WebPushSubscriptionRequest request = new WebPushSubscriptionRequest(
                "https://fcm.googleapis.com/fcm/send/sample-token-123",
                new PushKeys("sample-p256dh-key", "sample-auth-key"),
                "Mozilla/5.0 Chrome/130.0"
        );

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(subscriptionRepository.findByUserIdAndEndpoint(1L, request.endpoint())).willReturn(Optional.empty());
        given(subscriptionRepository.save(any(WebPushSubscription.class))).willAnswer(inv -> {
            WebPushSubscription sub = inv.getArgument(0);
            sub.setId(10L);
            return sub;
        });

        WebPushSubscription saved = webPushService.registerSubscription(1L, request);

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo(10L);
        assertThat(saved.getEndpoint()).isEqualTo(request.endpoint());
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getUser().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Unsubscribe deactivates existing push subscription")
    void unsubscribe_Success() {
        String endpoint = "https://fcm.googleapis.com/fcm/send/sample-token-123";
        WebPushSubscription existing = WebPushSubscription.builder()
                .id(10L)
                .user(user)
                .endpoint(endpoint)
                .active(true)
                .build();

        given(subscriptionRepository.findByUserIdAndEndpoint(1L, endpoint)).willReturn(Optional.of(existing));

        webPushService.unsubscribe(1L, endpoint);

        assertThat(existing.isActive()).isFalse();
        verify(subscriptionRepository).save(existing);
    }

    @Test
    @DisplayName("Get status returns active subscription count")
    void getStatus_Success() {
        WebPushSubscription sub1 = WebPushSubscription.builder().id(10L).active(true).build();
        given(subscriptionRepository.findByUserIdAndActiveTrue(1L)).willReturn(List.of(sub1));

        WebPushSubscriptionStatusResponse status = webPushService.getStatus(1L);

        assertThat(status.isSubscribed()).isTrue();
        assertThat(status.activeDeviceCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Get and update push preferences succeeds")
    void preferences_Success() {
        WebPushSubscription sub1 = WebPushSubscription.builder()
                .id(10L)
                .notifyGrades(true)
                .notifyClearance(true)
                .notifyHonors(true)
                .notifyAttendance(true)
                .active(true)
                .build();
        given(subscriptionRepository.findByUserIdAndActiveTrue(1L)).willReturn(List.of(sub1));

        PushPreferencesResponse prefs = webPushService.getPreferences(1L);
        assertThat(prefs.notifyGrades()).isTrue();
        assertThat(prefs.notifyClearance()).isTrue();

        PushPreferencesRequest updateReq = new PushPreferencesRequest(false, true, true, false);
        PushPreferencesResponse updated = webPushService.updatePreferences(1L, updateReq);

        assertThat(updated.notifyGrades()).isFalse();
        assertThat(updated.notifyAttendance()).isFalse();
        assertThat(sub1.isNotifyGrades()).isFalse();
        verify(subscriptionRepository).save(sub1);
    }

    @Test
    @DisplayName("Rotate subscription keys updates p256dh and auth keys and reactivates subscription")
    void rotateSubscriptionKeys_Success() {
        String endpoint = "https://fcm.googleapis.com/fcm/send/sample-token-123";
        WebPushSubscription existing = WebPushSubscription.builder()
                .id(10L)
                .user(user)
                .endpoint(endpoint)
                .p256dhKey("old-p256dh")
                .authKey("old-auth")
                .active(false)
                .build();

        RotatePushKeysRequest request = new RotatePushKeysRequest(
                endpoint,
                new PushKeys("new-p256dh-key", "new-auth-key")
        );

        given(subscriptionRepository.findByUserIdAndEndpoint(1L, endpoint)).willReturn(Optional.of(existing));
        given(subscriptionRepository.save(any(WebPushSubscription.class))).willAnswer(inv -> inv.getArgument(0));

        WebPushSubscription rotated = webPushService.rotateSubscriptionKeys(1L, request);

        assertThat(rotated).isNotNull();
        assertThat(rotated.getP256dhKey()).isEqualTo("new-p256dh-key");
        assertThat(rotated.getAuthKey()).isEqualTo("new-auth-key");
        assertThat(rotated.isActive()).isTrue();
        verify(subscriptionRepository).save(existing);
    }
}
