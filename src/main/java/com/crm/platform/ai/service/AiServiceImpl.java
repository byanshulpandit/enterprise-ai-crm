package com.crm.platform.ai.service;

import com.crm.platform.ai.client.GeminiClient;
import com.crm.platform.ai.dto.AiCampaignSummaryResponse;
import com.crm.platform.ai.dto.AiRuleGenerationRequest;
import com.crm.platform.ai.dto.AiRuleGenerationResponse;
import com.crm.platform.ai.dto.AiSegmentAuditDto;
import com.crm.platform.ai.entity.AiSegmentAudit;
import com.crm.platform.ai.repository.AiSegmentAuditRepository;
import com.crm.platform.campaign.entity.Campaign;
import com.crm.platform.campaign.repository.CampaignRepository;
import com.crm.platform.common.exception.InvalidRequestException;
import com.crm.platform.common.exception.ResourceNotFoundException;
import com.crm.platform.common.exception.UnprocessableEntityException;
import com.crm.platform.delivery.entity.DeliveryStatus;
import com.crm.platform.delivery.repository.CampaignDeliveryRecordRepository;
import com.crm.platform.segment.parser.SegmentRuleParser;
import com.crm.platform.user.entity.User;
import com.crm.platform.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AiServiceImpl implements AiService {

    private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);

    private final GeminiClient geminiClient;
    private final SegmentRuleParser segmentRuleParser;
    private final AiSegmentAuditRepository auditRepository;
    private final UserRepository userRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignDeliveryRecordRepository deliveryRecordRepository;
    private final ObjectMapper objectMapper;

    public AiServiceImpl(
            GeminiClient geminiClient,
            SegmentRuleParser segmentRuleParser,
            AiSegmentAuditRepository auditRepository,
            UserRepository userRepository,
            CampaignRepository campaignRepository,
            CampaignDeliveryRecordRepository deliveryRecordRepository,
            ObjectMapper objectMapper) {
        this.geminiClient = geminiClient;
        this.segmentRuleParser = segmentRuleParser;
        this.auditRepository = auditRepository;
        this.userRepository = userRepository;
        this.campaignRepository = campaignRepository;
        this.deliveryRecordRepository = deliveryRecordRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public AiRuleGenerationResponse generateSegmentRules(AiRuleGenerationRequest request, String callerUsername) {
        if (request == null || request.getPrompt() == null || request.getPrompt().trim().isEmpty()) {
            throw new InvalidRequestException("Prompt is required");
        }

        String rawPrompt = request.getPrompt().trim();
        String generatedJson = geminiClient.generateSegmentRulesJson(rawPrompt);

        // Validate generated AST against platform rule constraints
        try {
            segmentRuleParser.parse(generatedJson);
        } catch (Exception e) {
            log.warn("Generated rule tree failed AST validation: {}", e.getMessage());
            throw new UnprocessableEntityException("Generated rule tree fails AST validation: " + e.getMessage());
        }

        User user = resolveUser(callerUsername);

        // Record prompt, generated rules, and compliance audit trail
        AiSegmentAudit audit = new AiSegmentAudit(user, rawPrompt, generatedJson, "DISCARDED", null);
        auditRepository.save(audit);

        JsonNode ruleNode;
        try {
            ruleNode = objectMapper.readTree(generatedJson);
        } catch (Exception e) {
            throw new UnprocessableEntityException("Malformed JSON rule tree returned: " + e.getMessage());
        }

        return new AiRuleGenerationResponse(rawPrompt, ruleNode, true);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AiSegmentAuditDto> getAudits(Pageable pageable) {
        return auditRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(a -> AiSegmentAuditDto.fromEntity(a, objectMapper));
    }

    @Override
    public AiCampaignSummaryResponse generateCampaignAiSummary(Long campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + campaignId));

        long pending = deliveryRecordRepository.countByCampaignIdAndStatus(campaignId, DeliveryStatus.PENDING);
        long sent = deliveryRecordRepository.countByCampaignIdAndStatus(campaignId, DeliveryStatus.SENT);
        long failed = deliveryRecordRepository.countByCampaignIdAndStatus(campaignId, DeliveryStatus.FAILED);
        long total = pending + sent + failed;

        double deliveryRate = (total > 0) ? (sent * 100.0 / total) : 0.0;

        String metricsDesc = String.format(
                "Campaign '%s' (Status: %s). Target Audience: %d. Sent: %d, Failed: %d, Delivery Rate: %.2f%%. Launched: %s, Completed: %s",
                campaign.getName(),
                campaign.getStatus(),
                total,
                sent,
                failed,
                deliveryRate,
                campaign.getStartedAt() != null ? campaign.getStartedAt().toString() : "N/A",
                campaign.getCompletedAt() != null ? campaign.getCompletedAt().toString() : "In progress"
        );

        String narrative = geminiClient.generateCampaignSummary(metricsDesc);
        campaign.setAiSummary(narrative);
        campaignRepository.save(campaign);

        return new AiCampaignSummaryResponse(campaignId, narrative);
    }

    private User resolveUser(String username) {
        if (username == null || username.isBlank()) {
            throw new InvalidRequestException("Authenticated user identity is required");
        }
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));
    }
}
