package com.crm.platform.ai.service;

import com.crm.platform.ai.client.DefaultGeminiClient;
import com.crm.platform.ai.dto.AiRuleGenerationRequest;
import com.crm.platform.ai.dto.AiRuleGenerationResponse;
import com.crm.platform.ai.entity.AiSegmentAudit;
import com.crm.platform.ai.repository.AiSegmentAuditRepository;
import com.crm.platform.campaign.repository.CampaignRepository;
import com.crm.platform.common.exception.ServiceUnavailableException;
import com.crm.platform.delivery.repository.CampaignDeliveryRecordRepository;
import com.crm.platform.segment.parser.SegmentRuleParser;
import com.crm.platform.user.entity.RoleEnum;
import com.crm.platform.user.entity.User;
import com.crm.platform.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiHardeningTest {

    @Mock
    private AiSegmentAuditRepository auditRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CampaignDeliveryRecordRepository deliveryRecordRepository;

    private DefaultGeminiClient geminiClient;
    private AiServiceImpl aiService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        // Client without API key to trigger deterministic fallback
        geminiClient = new DefaultGeminiClient(
                "",
                "gemini-1.5-flash",
                3000L,
                7000L,
                objectMapper
        );

        SegmentRuleParser parser = new SegmentRuleParser(objectMapper);

        aiService = new AiServiceImpl(
                geminiClient,
                parser,
                auditRepository,
                userRepository,
                campaignRepository,
                deliveryRecordRepository,
                objectMapper
        );
    }

    @Test
    @DisplayName("Should flag isFallback=true when AI rule generation uses deterministic fallback")
    void testRuleGeneration_FallbackFlag() {
        User testUser = new User("marketer", "m@crm.internal", "pwd", RoleEnum.ROLE_MARKETER);
        when(userRepository.findByUsername("marketer")).thenReturn(Optional.of(testUser));
        when(auditRepository.save(any(AiSegmentAudit.class))).thenAnswer(inv -> inv.getArgument(0));

        AiRuleGenerationRequest request = new AiRuleGenerationRequest("Customers in Mumbai who spent over 5000");
        AiRuleGenerationResponse response = aiService.generateSegmentRules(request, "marketer");

        assertThat(response).isNotNull();
        assertThat(response.getIsValidated()).isTrue();
        assertThat(response.isFallback()).isTrue();
        assertThat(geminiClient.isLastGenerationFallback()).isTrue();
    }

    @Test
    @DisplayName("Should throw ServiceUnavailableException and never fabricate campaign summary when Gemini is unconfigured")
    void testCampaignSummary_Throws503WhenUnconfigured() {
        assertThatThrownBy(() -> geminiClient.generateCampaignSummary("100 sent, 5 failed"))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("Google Gemini AI API key is unconfigured. Campaign summary generation requires an active AI provider.");
    }
}
