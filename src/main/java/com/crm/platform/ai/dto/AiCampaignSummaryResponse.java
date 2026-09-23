package com.crm.platform.ai.dto;

public class AiCampaignSummaryResponse {

    private Long campaignId;
    private String aiSummary;

    public AiCampaignSummaryResponse() {
    }

    public AiCampaignSummaryResponse(Long campaignId, String aiSummary) {
        this.campaignId = campaignId;
        this.aiSummary = aiSummary;
    }

    public Long getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(Long campaignId) {
        this.campaignId = campaignId;
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public void setAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }
}
