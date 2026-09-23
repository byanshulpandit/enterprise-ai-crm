package com.crm.platform.ai.service;

import com.crm.platform.ai.dto.AiCampaignSummaryResponse;
import com.crm.platform.ai.dto.AiRuleGenerationRequest;
import com.crm.platform.ai.dto.AiRuleGenerationResponse;
import com.crm.platform.ai.dto.AiSegmentAuditDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AiService {

    AiRuleGenerationResponse generateSegmentRules(AiRuleGenerationRequest request, String callerUsername);

    Page<AiSegmentAuditDto> getAudits(Pageable pageable);

    AiCampaignSummaryResponse generateCampaignAiSummary(Long campaignId);
}
