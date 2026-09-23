package com.crm.platform.delivery.dto;

import com.crm.platform.delivery.entity.CampaignDeliveryRecord;
import com.crm.platform.delivery.entity.DeliveryStatus;

import java.time.Instant;

public class DeliveryRecordResponse {

    private Long id;
    private Long campaignId;
    private Long customerId;
    private String customerEmail;
    private String customerName;
    private String message;
    private DeliveryStatus status;
    private String failureReason;
    private Instant processedAt;
    private Instant createdAt;

    public DeliveryRecordResponse() {
    }

    public static DeliveryRecordResponse fromEntity(CampaignDeliveryRecord record) {
        DeliveryRecordResponse dto = new DeliveryRecordResponse();
        dto.setId(record.getId());
        dto.setCampaignId(record.getCampaign() != null ? record.getCampaign().getId() : null);
        if (record.getCustomer() != null) {
            dto.setCustomerId(record.getCustomer().getId());
            dto.setCustomerEmail(record.getCustomer().getEmail());
            dto.setCustomerName(record.getCustomer().getFirstName() + " " + record.getCustomer().getLastName());
        }
        dto.setMessage(record.getMessage());
        dto.setStatus(record.getStatus());
        dto.setFailureReason(record.getFailureReason());
        dto.setProcessedAt(record.getProcessedAt());
        dto.setCreatedAt(record.getCreatedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(Long campaignId) {
        this.campaignId = campaignId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public void setStatus(DeliveryStatus status) {
        this.status = status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
