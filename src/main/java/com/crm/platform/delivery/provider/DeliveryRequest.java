package com.crm.platform.delivery.provider;

public class DeliveryRequest {

    private final String idempotencyKey;
    private final Long campaignId;
    private final Long customerId;
    private final String recipientEmail;
    private final String messageText;

    public DeliveryRequest(String idempotencyKey, Long campaignId, Long customerId, String recipientEmail, String messageText) {
        this.idempotencyKey = idempotencyKey;
        this.campaignId = campaignId;
        this.customerId = customerId;
        this.recipientEmail = recipientEmail;
        this.messageText = messageText;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Long getCampaignId() {
        return campaignId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public String getMessageText() {
        return messageText;
    }
}
