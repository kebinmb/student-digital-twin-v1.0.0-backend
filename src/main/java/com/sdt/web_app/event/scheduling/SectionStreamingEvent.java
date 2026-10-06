package com.sdt.web_app.event.scheduling;

public record SectionStreamingEvent(
        Long termId,
        String eventName,
        Object data,
        String originNodeId
) {}
