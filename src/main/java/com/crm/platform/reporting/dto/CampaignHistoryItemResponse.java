package com.crm.platform.reporting.dto;

import com.crm.platform.campaign.entity.CampaignStatus;

import java.time.Instant;

public class CampaignHistoryItemResponse {

    private Long campaignId;
    private String campaignName;
    private CampaignStatus status;
    private long targetAudienceSize;
    private long sentCount;
    private long failedCount;
    private double deliveryRatePercentage;
    private Instant launchedAt;
    private Instant completedAt;

    public CampaignHistoryItemResponse() {
    }

    public CampaignHistoryItemResponse(Long campaignId, String campaignName, CampaignStatus status,
                                       long targetAudienceSize, long sentCount, long failedCount,
                                       double deliveryRatePercentage, Instant launchedAt, Instant completedAt) {
        this.campaignId = campaignId;
        this.campaignName = campaignName;
        this.status = status;
        this.targetAudienceSize = targetAudienceSize;
        this.sentCount = sentCount;
        this.failedCount = failedCount;
        this.deliveryRatePercentage = deliveryRatePercentage;
        this.launchedAt = launchedAt;
        this.completedAt = completedAt;
    }

    public Long getCampaignId() { return campaignId; }
    public void setCampaignId(Long campaignId) { this.campaignId = campaignId; }
    public String getCampaignName() { return campaignName; }
    public void setCampaignName(String campaignName) { this.campaignName = campaignName; }
    public CampaignStatus getStatus() { return status; }
    public void setStatus(CampaignStatus status) { this.status = status; }
    public long getTargetAudienceSize() { return targetAudienceSize; }
    public void setTargetAudienceSize(long targetAudienceSize) { this.targetAudienceSize = targetAudienceSize; }
    public long getSentCount() { return sentCount; }
    public void setSentCount(long sentCount) { this.sentCount = sentCount; }
    public long getFailedCount() { return failedCount; }
    public void setFailedCount(long failedCount) { this.failedCount = failedCount; }
    public double getDeliveryRatePercentage() { return deliveryRatePercentage; }
    public void setDeliveryRatePercentage(double deliveryRatePercentage) { this.deliveryRatePercentage = deliveryRatePercentage; }
    public Instant getLaunchedAt() { return launchedAt; }
    public void setLaunchedAt(Instant launchedAt) { this.launchedAt = launchedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
