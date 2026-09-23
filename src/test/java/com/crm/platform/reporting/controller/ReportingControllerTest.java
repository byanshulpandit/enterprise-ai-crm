package com.crm.platform.reporting.controller;

import com.crm.platform.campaign.entity.Campaign;
import com.crm.platform.campaign.entity.CampaignStatus;
import com.crm.platform.campaign.repository.CampaignRepository;
import com.crm.platform.segment.entity.Segment;
import com.crm.platform.segment.repository.SegmentRepository;
import com.crm.platform.user.entity.RoleEnum;
import com.crm.platform.user.entity.User;
import com.crm.platform.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ReportingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SegmentRepository segmentRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.crm.platform.ai.client.GeminiClient geminiClient;

    private Campaign testCampaign;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.when(geminiClient.generateCampaignSummary(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn("Executive AI Campaign Summary: Delivery demonstrated high reliability.");

        User marketer = userRepository.findByUsername("marketer_report_test").orElseGet(() -> {
            User u = new User("marketer_report_test", "marketer_report_test@crm.internal", "$2a$12$hash", RoleEnum.ROLE_MARKETER);
            return userRepository.save(u);
        });

        Segment segment = segmentRepository.findByName("Reporting Test Segment").orElseGet(() -> {
            Segment s = new Segment("Reporting Test Segment", "Desc", "{\"combinator\":\"AND\",\"conditions\":[]}", marketer);
            return segmentRepository.save(s);
        });

        testCampaign = campaignRepository.findByName("Reporting Test Campaign").orElseGet(() -> {
            Campaign c = new Campaign("Reporting Test Campaign", "Desc", segment, "Hello {{firstName}}", marketer);
            c.setStatus(CampaignStatus.COMPLETED);
            return campaignRepository.save(c);
        });
    }

    @Test
    @DisplayName("Should return 401 for unauthenticated customer overview request")
    void testCustomerOverviewUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/reports/customers/overview"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return customer overview report for authenticated MARKETER")
    @WithMockUser(username = "marketer_report_test", roles = {"MARKETER"})
    void testCustomerOverviewAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/reports/customers/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalActiveCustomers", notNullValue()))
                .andExpect(jsonPath("$.data.grossCustomerSpend", notNullValue()));
    }

    @Test
    @DisplayName("Should return campaign report for authenticated MARKETER")
    @WithMockUser(username = "marketer_report_test", roles = {"MARKETER"})
    void testCampaignReportAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/reports/campaigns/" + testCampaign.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.campaignId", is(testCampaign.getId().intValue())))
                .andExpect(jsonPath("$.data.campaignName", is("Reporting Test Campaign")));
    }

    @Test
    @DisplayName("Should return AI narrative summary for authenticated MARKETER")
    @WithMockUser(username = "marketer_report_test", roles = {"MARKETER"})
    void testCampaignAiSummaryAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/reports/campaigns/" + testCampaign.getId() + "/ai-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.campaignId", is(testCampaign.getId().intValue())))
                .andExpect(jsonPath("$.data.aiSummary", notNullValue()));
    }

    @Test
    @DisplayName("Should return paginated campaign history for authenticated MARKETER")
    @WithMockUser(username = "marketer_report_test", roles = {"MARKETER"})
    void testCampaignHistoryAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/reports/campaigns/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", notNullValue()))
                .andExpect(jsonPath("$.metadata.pagination", notNullValue()));
    }
}
