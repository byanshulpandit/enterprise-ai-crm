package com.crm.platform.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AiRuleGenerationRequest {

    @NotBlank(message = "Prompt must not be blank")
    @Size(min = 10, max = 500, message = "Prompt must be between 10 and 500 characters")
    private String prompt;

    public AiRuleGenerationRequest() {
    }

    public AiRuleGenerationRequest(String prompt) {
        this.prompt = prompt;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
}
