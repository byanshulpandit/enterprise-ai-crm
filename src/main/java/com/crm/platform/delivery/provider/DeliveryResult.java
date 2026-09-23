package com.crm.platform.delivery.provider;

import java.time.Instant;

public class DeliveryResult {

    private final boolean success;
    private final String messageId;
    private final String failureReason;
    private final Instant processedAt;

    private DeliveryResult(boolean success, String messageId, String failureReason, Instant processedAt) {
        this.success = success;
        this.messageId = messageId;
        this.failureReason = failureReason;
        this.processedAt = processedAt;
    }

    public static DeliveryResult success(String messageId) {
        return new DeliveryResult(true, messageId, null, Instant.now());
    }

    public static DeliveryResult failure(String failureReason) {
        return new DeliveryResult(false, null, failureReason, Instant.now());
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
