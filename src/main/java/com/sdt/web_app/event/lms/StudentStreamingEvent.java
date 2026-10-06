package com.sdt.web_app.event.lms;

public record StudentStreamingEvent(
        Long studentId,
        String eventName,
        Object data,
        String originNodeId
) {}
