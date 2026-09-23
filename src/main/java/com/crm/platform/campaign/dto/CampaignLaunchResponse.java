package com.crm.platform.campaign.dto;

import com.crm.platform.campaign.entity.CampaignStatus;

import java.time.Instant;

public class CampaignLaunchResponse {

    private Long campaignId;
    private CampaignStatus status;
    private long targetAudienceSize;
    private String message;
    private Instant launchedAt;

    public CampaignLaunchResponse() {
    }

    public CampaignLaunchResponse(Long campaignId, CampaignStatus status, long targetAudienceSize, String message, Instant launchedAt) {
        this.campaignId = campaignId;
        this.status = status;
        this.targetAudienceSize = targetAudienceSize;
        this.message = message;
        this.launchedAt = launchedAt;
    }

    public Long getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(Long campaignId) {
        this.campaignId = campaignId;
    }

    public CampaignStatus getStatus() {
        return status;
    }

    public void setStatus(CampaignStatus status) {
        this.status = status;
    }

    public long getTargetAudienceSize() {
        return targetAudienceSize;
    }

    public void setTargetAudienceSize(long targetAudienceSize) {
        this.targetAudienceSize = targetAudienceSize;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getLaunchedAt() {
        return launchedAt;
    }

    public void setLaunchedAt(Instant launchedAt) {
        this.launchedAt = launchedAt;
    }
}
