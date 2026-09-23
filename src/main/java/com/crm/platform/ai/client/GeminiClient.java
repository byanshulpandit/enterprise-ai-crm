package com.crm.platform.ai.client;

public interface GeminiClient {

    String generateSegmentRulesJson(String naturalLanguagePrompt);

    String generateCampaignSummary(String campaignMetricsDescription);
}
