package com.crm.platform.ai.client;

import com.crm.platform.common.exception.ServiceUnavailableException;
import com.crm.platform.common.exception.UnprocessableEntityException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultGeminiClientTest {

    private DefaultGeminiClient client;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        client = new DefaultGeminiClient("", "gemini-1.5-flash", 1000L, 1000L, objectMapper);
    }

    @Test
    @DisplayName("extractTextFromGeminiResponse successfully extracts text from valid candidate payload")
    void testExtractText_ValidPayload() {
        String validJson = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"The campaign executed with 98% delivery rate.\"}]}}]}";
        String result = client.extractTextFromGeminiResponse(validJson);
        assertThat(result).isEqualTo("The campaign executed with 98% delivery rate.");
    }

    @Test
    @DisplayName("extractTextFromGeminiResponse throws ServiceUnavailableException on empty candidate parts")
    void testExtractText_EmptyCandidateParts_Throws503() {
        String emptyParts = "{\"candidates\":[{\"content\":{\"parts\":[]}}]}";
        assertThatThrownBy(() -> client.extractTextFromGeminiResponse(emptyParts))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("Gemini response did not contain valid summary text content");
    }

    @Test
    @DisplayName("extractTextFromGeminiResponse throws ServiceUnavailableException on malformed JSON")
    void testExtractText_MalformedJson_Throws503() {
        String malformed = "{invalid-json-response}";
        assertThatThrownBy(() -> client.extractTextFromGeminiResponse(malformed))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("Failed to extract campaign summary from Gemini response");
    }

    @Test
    @DisplayName("extractJsonFromGeminiResponse extracts AST JSON block from markdown wrapped response")
    void testExtractJson_MarkdownBlock() {
        String geminiResponse = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"```json\\n{\\\"combinator\\\": \\\"AND\\\", \\\"conditions\\\": []}\\n```\"}]}}]}";
        String extracted = client.extractJsonFromGeminiResponse(geminiResponse);
        assertThat(extracted).isEqualTo("{\"combinator\": \"AND\", \"conditions\": []}");
    }

    @Test
    @DisplayName("extractJsonFromGeminiResponse throws UnprocessableEntityException on malformed payload")
    void testExtractJson_MalformedPayload_Throws422() {
        String bad = "not-json";
        assertThatThrownBy(() -> client.extractJsonFromGeminiResponse(bad))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("Failed to extract structured JSON rule tree from Gemini response");
    }

    @Test
    @DisplayName("generateSegmentRulesJson uses deterministic fallback when API key is empty")
    void testDeterministicFallback() {
        String prompt = "Customers in Delhi who spent over 2500";
        String json = client.generateSegmentRulesJson(prompt);
        assertThat(json).contains("Delhi");
        assertThat(json).contains("2500");
        assertThat(client.isLastGenerationFallback()).isTrue();
    }
}
