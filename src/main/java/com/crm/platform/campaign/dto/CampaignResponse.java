package com.crm.platform.campaign.dto;

import com.crm.platform.campaign.entity.Campaign;
import com.crm.platform.campaign.entity.CampaignStatus;

import java.time.Instant;

public class CampaignResponse {

    private Long id;
    private String name;
    private String description;
    private Long segmentId;
    private String segmentName;
    private String messageTemplate;
    private CampaignStatus status;
    private Boolean personalizationEnabled;
    private String aiSummary;
    private Long createdBy;
    private String createdByName;
    private Instant startedAt;
    private Instant completedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public CampaignResponse() {
    }

    public CampaignResponse(Long id, String name, String description, Long segmentId,
                            String segmentName, String messageTemplate, CampaignStatus status,
                            Boolean personalizationEnabled, String aiSummary, Long createdBy,
                            String createdByName, Instant startedAt, Instant completedAt,
                            Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.segmentId = segmentId;
        this.segmentName = segmentName;
        this.messageTemplate = messageTemplate;
        this.status = status;
        this.personalizationEnabled = personalizationEnabled;
        this.aiSummary = aiSummary;
        this.createdBy = createdBy;
        this.createdByName = createdByName;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static CampaignResponse fromEntity(Campaign campaign) {
        if (campaign == null) {
            return null;
        }

        Long segId = null;
        String segName = null;
        if (campaign.getSegment() != null) {
            segId = campaign.getSegment().getId();
            try {
                segName = campaign.getSegment().getName();
            } catch (Exception ignored) {
            }
        }

        Long creatorId = null;
        String creatorUsername = null;
        if (campaign.getCreatedBy() != null) {
            creatorId = campaign.getCreatedBy().getId();
            try {
                creatorUsername = campaign.getCreatedBy().getUsername();
            } catch (Exception ignored) {
            }
        }

        return new CampaignResponse(
                campaign.getId(),
                campaign.getName(),
                campaign.getDescription(),
                segId,
                segName,
                campaign.getMessageTemplate(),
                campaign.getStatus(),
                campaign.getPersonalizationEnabled(),
                campaign.getAiSummary(),
                creatorId,
                creatorUsername,
                campaign.getStartedAt(),
                campaign.getCompletedAt(),
                campaign.getCreatedAt(),
                campaign.getUpdatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getSegmentName() {
        return segmentName;
    }

    public void setSegmentName(String segmentName) {
        this.segmentName = segmentName;
    }

    public String getMessageTemplate() {
        return messageTemplate;
    }

    public void setMessageTemplate(String messageTemplate) {
        this.messageTemplate = messageTemplate;
    }

    public CampaignStatus getStatus() {
        return status;
    }

    public void setStatus(CampaignStatus status) {
        this.status = status;
    }

    public Boolean getPersonalizationEnabled() {
        return personalizationEnabled;
    }

    public void setPersonalizationEnabled(Boolean personalizationEnabled) {
        this.personalizationEnabled = personalizationEnabled;
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public void setAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
