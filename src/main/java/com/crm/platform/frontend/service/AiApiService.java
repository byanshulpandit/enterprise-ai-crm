package com.crm.platform.frontend.service;

import com.crm.platform.ai.dto.AiRuleGenerationRequest;
import com.crm.platform.ai.dto.AiRuleGenerationResponse;
import com.crm.platform.ai.dto.AiSegmentAuditDto;
import com.crm.platform.frontend.client.ApiEndpointConstants;
import com.crm.platform.frontend.client.BackendApiClient;
import com.crm.platform.frontend.client.PageResult;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service invoking backend AI REST endpoints.
 */
@Service
public class AiApiService {

    private final BackendApiClient apiClient;

    public AiApiService(BackendApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public AiRuleGenerationResponse generateSegmentRules(String prompt) {
        AiRuleGenerationRequest request = new AiRuleGenerationRequest(prompt);
        return apiClient.post(ApiEndpointConstants.AI_GENERATE_RULES, request, AiRuleGenerationResponse.class);
    }

    public PageResult<AiSegmentAuditDto> getAiAudits(int page, int size) {
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("page", page);
        queryParams.put("size", size);
        return apiClient.getPage(ApiEndpointConstants.AI_AUDITS, AiSegmentAuditDto.class, queryParams);
    }
}
