package com.crm.platform.frontend.service;

import com.crm.platform.ai.dto.AiCampaignSummaryResponse;
import com.crm.platform.frontend.client.ApiEndpointConstants;
import com.crm.platform.frontend.client.BackendApiClient;
import com.crm.platform.frontend.client.PageResult;
import com.crm.platform.reporting.dto.CampaignHistoryItemResponse;
import com.crm.platform.reporting.dto.CampaignReportResponse;
import com.crm.platform.reporting.dto.CustomerOverviewReportResponse;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service invoking backend Business Intelligence & Reporting REST endpoints.
 */
@Service
public class ReportApiService {

    private final BackendApiClient apiClient;

    public ReportApiService(BackendApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public CustomerOverviewReportResponse getCustomerOverview() {
        return apiClient.get(ApiEndpointConstants.REPORTS_CUSTOMER_OVERVIEW, CustomerOverviewReportResponse.class);
    }

    public CampaignReportResponse getCampaignReport(Long campaignId) {
        return apiClient.get(ApiEndpointConstants.REPORTS_CAMPAIGN_BY_ID, CampaignReportResponse.class, campaignId);
    }

    public AiCampaignSummaryResponse getCampaignAiSummary(Long campaignId) {
        return apiClient.get(ApiEndpointConstants.REPORTS_CAMPAIGN_AI_SUMMARY, AiCampaignSummaryResponse.class, campaignId);
    }

    public PageResult<CampaignHistoryItemResponse> getCampaignHistory(int page, int size) {
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("page", page);
        queryParams.put("size", size);
        return apiClient.getPage("/api/v1/reports/campaigns/history", CampaignHistoryItemResponse.class, queryParams);
    }
}
