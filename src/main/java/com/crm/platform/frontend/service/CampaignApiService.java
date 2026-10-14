package com.crm.platform.frontend.service;

import com.crm.platform.campaign.dto.CampaignCreateRequest;
import com.crm.platform.campaign.dto.CampaignLaunchResponse;
import com.crm.platform.campaign.dto.CampaignResponse;
import com.crm.platform.campaign.dto.CampaignUpdateRequest;
import com.crm.platform.campaign.entity.CampaignStatus;
import com.crm.platform.frontend.client.ApiEndpointConstants;
import com.crm.platform.frontend.client.BackendApiClient;
import com.crm.platform.frontend.client.PageResult;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service invoking backend Campaign REST endpoints.
 */
@Service
public class CampaignApiService {

    private final BackendApiClient apiClient;

    public CampaignApiService(BackendApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public CampaignResponse createCampaign(CampaignCreateRequest request) {
        return apiClient.post(ApiEndpointConstants.CAMPAIGNS, request, CampaignResponse.class);
    }

    public CampaignResponse getCampaignById(Long id) {
        return apiClient.get(ApiEndpointConstants.CAMPAIGN_BY_ID, CampaignResponse.class, id);
    }

    public CampaignResponse updateCampaign(Long id, CampaignUpdateRequest request) {
        return apiClient.patch(ApiEndpointConstants.CAMPAIGN_BY_ID, request, CampaignResponse.class, id);
    }

    public void deleteCampaign(Long id) {
        apiClient.delete(ApiEndpointConstants.CAMPAIGN_BY_ID, id);
    }

    public PageResult<CampaignResponse> listCampaigns(CampaignStatus status, int page, int size) {
        Map<String, Object> queryParams = new HashMap<>();
        if (status != null) {
            queryParams.put("status", status.name());
        }
        queryParams.put("page", page);
        queryParams.put("size", size);
        return apiClient.getPage(ApiEndpointConstants.CAMPAIGNS, CampaignResponse.class, queryParams);
    }

    public CampaignLaunchResponse launchCampaign(Long id) {
        return apiClient.post(ApiEndpointConstants.CAMPAIGN_LAUNCH, null, CampaignLaunchResponse.class, id);
    }
}
