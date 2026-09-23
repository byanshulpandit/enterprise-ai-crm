package com.crm.platform.delivery.dto;

import com.crm.platform.campaign.entity.CampaignStatus;

public class DeliverySummaryResponse {

    private Long campaignId;
    private CampaignStatus campaignStatus;
    private long targetAudienceSize;
    private long pendingCount;
    private long sentCount;
    private long failedCount;
    private double completionPercentage;
    private boolean isTerminal;

    public DeliverySummaryResponse() {
    }

    public DeliverySummaryResponse(Long campaignId, CampaignStatus campaignStatus, long targetAudienceSize,
                                   long pendingCount, long sentCount, long failedCount,
                                   double completionPercentage, boolean isTerminal) {
        this.campaignId = campaignId;
        this.campaignStatus = campaignStatus;
        this.targetAudienceSize = targetAudienceSize;
        this.pendingCount = pendingCount;
        this.sentCount = sentCount;
        this.failedCount = failedCount;
        this.completionPercentage = completionPercentage;
        this.isTerminal = isTerminal;
    }

    public Long getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(Long campaignId) {
        this.campaignId = campaignId;
    }

    public CampaignStatus getCampaignStatus() {
        return campaignStatus;
    }

    public void setCampaignStatus(CampaignStatus campaignStatus) {
        this.campaignStatus = campaignStatus;
    }

    public long getTargetAudienceSize() {
        return targetAudienceSize;
    }

    public void setTargetAudienceSize(long targetAudienceSize) {
        this.targetAudienceSize = targetAudienceSize;
    }

    public long getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(long pendingCount) {
        this.pendingCount = pendingCount;
    }

    public long getSentCount() {
        return sentCount;
    }

    public void setSentCount(long sentCount) {
        this.sentCount = sentCount;
    }

    public long getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(long failedCount) {
        this.failedCount = failedCount;
    }

    public double getCompletionPercentage() {
        return completionPercentage;
    }

    public void setCompletionPercentage(double completionPercentage) {
        this.completionPercentage = completionPercentage;
    }

    public boolean isTerminal() {
        return isTerminal;
    }

    public void setTerminal(boolean terminal) {
        isTerminal = terminal;
    }
}
