package com.crm.platform.frontend.service;

import com.crm.platform.delivery.dto.DeliveryRecordResponse;
import com.crm.platform.delivery.dto.DeliverySummaryResponse;
import com.crm.platform.delivery.entity.DeliveryStatus;
import com.crm.platform.frontend.client.ApiEndpointConstants;
import com.crm.platform.frontend.client.BackendApiClient;
import com.crm.platform.frontend.client.PageResult;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service invoking backend Delivery REST endpoints for telemetry and delivery records.
 */
@Service
public class DeliveryApiService {

    private final BackendApiClient apiClient;

    public DeliveryApiService(BackendApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public DeliverySummaryResponse getDeliverySummary(Long campaignId) {
        return apiClient.get(ApiEndpointConstants.CAMPAIGN_DELIVERY_SUMMARY, DeliverySummaryResponse.class, campaignId);
    }

    public PageResult<DeliveryRecordResponse> getDeliveries(Long campaignId, DeliveryStatus status, int page, int size) {
        Map<String, Object> queryParams = new HashMap<>();
        if (status != null) {
            queryParams.put("status", status.name());
        }
        queryParams.put("page", page);
        queryParams.put("size", size);
        return apiClient.getPage(ApiEndpointConstants.DELIVERIES_BY_CAMPAIGN, DeliveryRecordResponse.class, queryParams, campaignId);
    }
}
