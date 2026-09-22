package com.crm.platform.campaign.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class CampaignUpdateRequest {

    @Size(max = 100, message = "Campaign name must not exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Campaign description must not exceed 500 characters")
    private String description;

    @Positive(message = "Segment ID must be positive")
    private Long segmentId;

    @Size(max = 1000, message = "Message template must not exceed 1000 characters")
    private String messageTemplate;

    private Boolean personalizationEnabled;

    public CampaignUpdateRequest() {
    }

    public CampaignUpdateRequest(String name, String description, Long segmentId,
                                 String messageTemplate, Boolean personalizationEnabled) {
        this.name = name;
        this.description = description;
        this.segmentId = segmentId;
        this.messageTemplate = messageTemplate;
        this.personalizationEnabled = personalizationEnabled;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getSegmentId() {
        return segmentId;
    }

    public void setSegmentId(Long segmentId) {
        this.segmentId = segmentId;
    }

    public String getMessageTemplate() {
        return messageTemplate;
    }

    public void setMessageTemplate(String messageTemplate) {
        this.messageTemplate = messageTemplate;
    }

    public Boolean getPersonalizationEnabled() {
        return personalizationEnabled;
    }

    public void setPersonalizationEnabled(Boolean personalizationEnabled) {
        this.personalizationEnabled = personalizationEnabled;
    }
}
