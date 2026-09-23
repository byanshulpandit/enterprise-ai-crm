package com.crm.platform.ai.dto;

import com.fasterxml.jackson.databind.JsonNode;

public class AiRuleGenerationResponse {

    private String prompt;
    private JsonNode ruleTree;
    private boolean isValidated;

    public AiRuleGenerationResponse() {
    }

    public AiRuleGenerationResponse(String prompt, JsonNode ruleTree, boolean isValidated) {
        this.prompt = prompt;
        this.ruleTree = ruleTree;
        this.isValidated = isValidated;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public JsonNode getRuleTree() {
        return ruleTree;
    }

    public void setRuleTree(JsonNode ruleTree) {
        this.ruleTree = ruleTree;
    }

    public boolean getIsValidated() {
        return isValidated;
    }

    public void setIsValidated(boolean validated) {
        isValidated = validated;
    }
}
