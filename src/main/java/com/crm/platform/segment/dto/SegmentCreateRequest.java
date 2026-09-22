package com.crm.platform.segment.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class SegmentCreateRequest {

    @NotBlank(message = "Segment name must not be blank")
    @Size(max = 100, message = "Segment name must not exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Segment description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Segment rules must not be null")
    @JsonAlias({"ruleTree", "rules"})
    private JsonNode rules;

    public SegmentCreateRequest() {
    }

    public SegmentCreateRequest(String name, String description, JsonNode rules) {
        this.name = name;
        this.description = description;
        this.rules = rules;
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
}
