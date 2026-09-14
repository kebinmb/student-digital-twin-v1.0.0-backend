package com.sdt.web_app.exceptions;

public class QueueSessionExpiredException extends RuntimeException {
    public QueueSessionExpiredException(String message) {
        super(message);
    }
}
