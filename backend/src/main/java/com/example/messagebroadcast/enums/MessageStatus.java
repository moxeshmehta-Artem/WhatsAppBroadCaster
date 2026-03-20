package com.example.messagebroadcast.enums;

/**
 * Centralized constant for message statuses.
 * SENT = Provider accepted the message and returned an external ID.
 * FAILED = Provider rejected the message or returned an error.
 */
public enum MessageStatus {
    SENT,
    FAILED
}
