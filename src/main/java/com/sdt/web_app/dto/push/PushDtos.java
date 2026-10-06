package com.sdt.web_app.dto.push;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class PushDtos {

    private PushDtos() {}

    public record VapidPublicKeyResponse(
            String publicKey
    ) {}

    public record PushKeys(
            @NotBlank(message = "p256dh key is required")
            String p256dh,

            @NotBlank(message = "auth key is required")
            String auth
    ) {}

    public record WebPushSubscriptionRequest(
            @NotBlank(message = "Endpoint is required")
            String endpoint,

            @NotNull(message = "Keys object is required")
            PushKeys keys,

            String userAgent
    ) {}

    public record UnsubscribeRequest(
            @NotBlank(message = "Endpoint is required")
            String endpoint
    ) {}

    public record RotatePushKeysRequest(
            @NotBlank(message = "Endpoint is required")
            String endpoint,

            @NotNull(message = "New keys object is required")
            PushKeys keys
    ) {}

    public record WebPushSubscriptionStatusResponse(
            boolean isSubscribed,
            int activeDeviceCount
    ) {}

    public record TestPushNotificationRequest(
            String title,
            String body,
            String url
    ) {}

    public record PushPreferencesRequest(
            boolean notifyGrades,
            boolean notifyClearance,
            boolean notifyHonors,
            boolean notifyAttendance
    ) {}

    public record PushPreferencesResponse(
            boolean notifyGrades,
            boolean notifyClearance,
            boolean notifyHonors,
            boolean notifyAttendance
    ) {}
}
