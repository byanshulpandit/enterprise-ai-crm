package com.crm.platform.frontend.service;

import com.crm.platform.customer.dto.CustomerResponseDto;
import com.crm.platform.frontend.client.ApiEndpointConstants;
import com.crm.platform.frontend.client.BackendApiClient;
import com.crm.platform.frontend.client.PageResult;
import com.crm.platform.segment.dto.SegmentCreateRequest;
import com.crm.platform.segment.dto.SegmentPreviewResponse;
import com.crm.platform.segment.dto.SegmentResponse;
import com.crm.platform.segment.dto.SegmentUpdateRequest;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service invoking backend Segment REST endpoints.
 */
@Service
public class SegmentApiService {

    private final BackendApiClient apiClient;

    public SegmentApiService(BackendApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public SegmentResponse createSegment(SegmentCreateRequest request) {
        return apiClient.post(ApiEndpointConstants.SEGMENTS, request, SegmentResponse.class);
    }

    public SegmentResponse getSegmentById(Long id) {
        return apiClient.get(ApiEndpointConstants.SEGMENT_BY_ID, SegmentResponse.class, id);
    }

    public SegmentResponse updateSegment(Long id, SegmentUpdateRequest request) {
        return apiClient.patch(ApiEndpointConstants.SEGMENT_BY_ID, request, SegmentResponse.class, id);
    }

    public void deleteSegment(Long id) {
        apiClient.delete(ApiEndpointConstants.SEGMENT_BY_ID, id);
    }

    public PageResult<SegmentResponse> listSegments(int page, int size) {
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("page", page);
        queryParams.put("size", size);
        return apiClient.getPage(ApiEndpointConstants.SEGMENTS, SegmentResponse.class, queryParams);
    }

    public SegmentPreviewResponse previewSegment(Long id) {
        return apiClient.post(ApiEndpointConstants.SEGMENT_PREVIEW, null, SegmentPreviewResponse.class, id);
    }

    public PageResult<CustomerResponseDto> getSegmentMembers(Long id, int page, int size) {
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("page", page);
        queryParams.put("size", size);
        return apiClient.getPage(ApiEndpointConstants.SEGMENT_MEMBERS, CustomerResponseDto.class, queryParams, id);
    }
}
