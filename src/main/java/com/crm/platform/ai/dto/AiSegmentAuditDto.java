package com.crm.platform.ai.dto;

import com.crm.platform.ai.entity.AiSegmentAudit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;

public class AiSegmentAuditDto {

    private Long id;
    private String username;
    private String promptText;
    private JsonNode generatedRules;
    private String actionTaken;
    private Long segmentId;
    private Instant createdAt;

    public AiSegmentAuditDto() {
    }

    public static AiSegmentAuditDto fromEntity(AiSegmentAudit audit, ObjectMapper objectMapper) {
        AiSegmentAuditDto dto = new AiSegmentAuditDto();
        dto.setId(audit.getId());
        dto.setUsername(audit.getUser() != null ? audit.getUser().getUsername() : null);
        dto.setPromptText(audit.getPromptText());
        try {
            dto.setGeneratedRules(objectMapper.readTree(audit.getGeneratedRules()));
        } catch (Exception e) {
            dto.setGeneratedRules(null);
        }
        dto.setActionTaken(audit.getActionTaken());
        dto.setSegmentId(audit.getSegment() != null ? audit.getSegment().getId() : null);
        dto.setCreatedAt(audit.getCreatedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPromptText() {
        return promptText;
    }

    public void setPromptText(String promptText) {
        this.promptText = promptText;
    }

    public JsonNode getGeneratedRules() {
        return generatedRules;
    }

    public void setGeneratedRules(JsonNode generatedRules) {
        this.generatedRules = generatedRules;
    }

    public String getActionTaken() {
        return actionTaken;
    }

    public void setActionTaken(String actionTaken) {
        this.actionTaken = actionTaken;
    }

    public Long getSegmentId() {
        return segmentId;
    }

    public void setSegmentId(Long segmentId) {
        this.segmentId = segmentId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
