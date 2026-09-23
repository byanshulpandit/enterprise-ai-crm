package com.crm.platform.ai.service;

import com.crm.platform.ai.client.GeminiClient;
import com.crm.platform.ai.dto.AiCampaignSummaryResponse;
import com.crm.platform.ai.dto.AiRuleGenerationRequest;
import com.crm.platform.ai.dto.AiRuleGenerationResponse;
import com.crm.platform.ai.entity.AiSegmentAudit;
import com.crm.platform.ai.repository.AiSegmentAuditRepository;
import com.crm.platform.campaign.entity.Campaign;
import com.crm.platform.campaign.entity.CampaignStatus;
import com.crm.platform.campaign.repository.CampaignRepository;
import com.crm.platform.common.exception.InvalidRequestException;
import com.crm.platform.common.exception.UnprocessableEntityException;
import com.crm.platform.delivery.entity.DeliveryStatus;
import com.crm.platform.delivery.repository.CampaignDeliveryRecordRepository;
import com.crm.platform.segment.model.RuleNode;
import com.crm.platform.segment.parser.SegmentRuleParser;
import com.crm.platform.user.entity.RoleEnum;
import com.crm.platform.user.entity.User;
import com.crm.platform.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiServiceTest {

    @Mock
    private GeminiClient geminiClient;

    @Mock
    private SegmentRuleParser segmentRuleParser;

    @Mock
    private AiSegmentAuditRepository auditRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CampaignDeliveryRecordRepository deliveryRecordRepository;

    private AiServiceImpl aiService;
    private User testUser;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        aiService = new AiServiceImpl(
                geminiClient,
                segmentRuleParser,
                auditRepository,
                userRepository,
                campaignRepository,
                deliveryRecordRepository,
                objectMapper
        );

        testUser = new User("admin_ai", "admin_ai@crm.internal", "hash", RoleEnum.ROLE_ADMIN);
        testUser.setId(1L);
    }

    @Test
    @DisplayName("Should successfully generate validated AST and record compliance audit")
    void testGenerateSegmentRules_Success() {
        String prompt = "Target customers living in Mumbai who spent over 15000";
        String validJson = "{\"combinator\":\"AND\",\"conditions\":[{\"field\":\"city\",\"operator\":\"EQUALS\",\"value\":\"Mumbai\"}]}";

        when(geminiClient.generateSegmentRulesJson(prompt)).thenReturn(validJson);
        when(segmentRuleParser.parse(validJson)).thenReturn(mock(RuleNode.class));
        when(userRepository.findByUsername("admin_ai")).thenReturn(Optional.of(testUser));

        AiRuleGenerationRequest request = new AiRuleGenerationRequest(prompt);
        AiRuleGenerationResponse response = aiService.generateSegmentRules(request, "admin_ai");

        assertThat(response).isNotNull();
        assertThat(response.getPrompt()).isEqualTo(prompt);
        assertThat(response.getIsValidated()).isTrue();
        assertThat(response.getRuleTree().get("combinator").asText()).isEqualTo("AND");

        ArgumentCaptor<AiSegmentAudit> captor = ArgumentCaptor.forClass(AiSegmentAudit.class);
        verify(auditRepository).save(captor.capture());
        AiSegmentAudit audit = captor.getValue();
        assertThat(audit.getPromptText()).isEqualTo(prompt);
        assertThat(audit.getGeneratedRules()).isEqualTo(validJson);
        assertThat(audit.getActionTaken()).isEqualTo("DISCARDED");
    }

    @Test
    @DisplayName("Should throw UnprocessableEntityException when generated AST fails validation")
    void testGenerateSegmentRules_FailsValidation() {
        String prompt = "Invalid prompt resulting in bad AST";
        String invalidJson = "{\"badField\":\"badValue\"}";

        when(geminiClient.generateSegmentRulesJson(prompt)).thenReturn(invalidJson);
        when(segmentRuleParser.parse(invalidJson)).thenThrow(new InvalidRequestException("Missing combinator or conditions"));

        AiRuleGenerationRequest request = new AiRuleGenerationRequest(prompt);

        assertThatThrownBy(() -> aiService.generateSegmentRules(request, "admin_ai"))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("Generated rule tree fails AST validation");

        verify(auditRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject blank prompt with InvalidRequestException")
    void testGenerateSegmentRules_BlankPrompt() {
        AiRuleGenerationRequest request = new AiRuleGenerationRequest("   ");

        assertThatThrownBy(() -> aiService.generateSegmentRules(request, "admin_ai"))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    @DisplayName("Should generate and persist AI campaign narrative summary")
    void testGenerateCampaignAiSummary_Success() {
        Campaign campaign = new Campaign();
        campaign.setId(10L);
        campaign.setName("Diwali Flash Sale");
        campaign.setStatus(CampaignStatus.COMPLETED);

        when(campaignRepository.findById(10L)).thenReturn(Optional.of(campaign));
        when(deliveryRecordRepository.countByCampaignIdAndStatus(10L, DeliveryStatus.PENDING)).thenReturn(0L);
        when(deliveryRecordRepository.countByCampaignIdAndStatus(10L, DeliveryStatus.SENT)).thenReturn(95L);
        when(deliveryRecordRepository.countByCampaignIdAndStatus(10L, DeliveryStatus.FAILED)).thenReturn(5L);
        when(geminiClient.generateCampaignSummary(anyString())).thenReturn("The campaign was a great success with a 95% delivery rate.");

        AiCampaignSummaryResponse response = aiService.generateCampaignAiSummary(10L);

        assertThat(response).isNotNull();
        assertThat(response.getCampaignId()).isEqualTo(10L);
        assertThat(response.getAiSummary()).contains("95% delivery rate");
        assertThat(campaign.getAiSummary()).contains("95% delivery rate");
        verify(campaignRepository).save(campaign);
    }
}
