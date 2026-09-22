package com.crm.platform.segment.dto;

import com.crm.platform.segment.entity.Segment;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;

public class SegmentResponse {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private Long id;
    private String name;
    private String description;
    private JsonNode rules;
    private Long createdBy;
    private String createdByName;
    private Instant createdAt;
    private Instant updatedAt;

    public SegmentResponse() {
    }

    public SegmentResponse(Long id, String name, String description, JsonNode rules,
                           Long createdBy, String createdByName, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.rules = rules;
        this.createdBy = createdBy;
        this.createdByName = createdByName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static SegmentResponse fromEntity(Segment segment) {
        if (segment == null) {
            return null;
        }
        JsonNode rulesNode = null;
        if (segment.getRules() != null && !segment.getRules().isBlank()) {
            try {
                rulesNode = OBJECT_MAPPER.readTree(segment.getRules());
            } catch (Exception e) {
                rulesNode = OBJECT_MAPPER.getNodeFactory().textNode(segment.getRules());
            }
        }

        Long creatorId = null;
        String creatorUsername = null;
        if (segment.getCreatedBy() != null) {
            creatorId = segment.getCreatedBy().getId();
            try {
                creatorUsername = segment.getCreatedBy().getUsername();
            } catch (Exception ignored) {
            }
        }

        return new SegmentResponse(
                segment.getId(),
                segment.getName(),
                segment.getDescription(),
                rulesNode,
                creatorId,
                creatorUsername,
                segment.getCreatedAt(),
                segment.getUpdatedAt()
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

    public JsonNode getRules() {
        return rules;
    }

    public void setRules(JsonNode rules) {
        this.rules = rules;
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
