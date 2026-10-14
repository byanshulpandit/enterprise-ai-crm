package com.crm.platform.frontend.service;

import com.crm.platform.frontend.client.ApiEndpointConstants;
import com.crm.platform.frontend.client.BackendApiClient;
import com.crm.platform.frontend.client.PageResult;
import com.crm.platform.upload.dto.UploadHistoryDto;
import com.crm.platform.upload.dto.UploadResultResponse;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service invoking backend Bulk Ingestion REST endpoints.
 */
@Service
public class UploadApiService {

    private final BackendApiClient apiClient;

    public UploadApiService(BackendApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public UploadResultResponse bulkUpload(String filename, byte[] content) {
        return apiClient.uploadMultipart(
                ApiEndpointConstants.UPLOADS_BULK,
                "file",
                filename,
                content,
                UploadResultResponse.class
        );
    }

    public PageResult<UploadHistoryDto> getUploadHistory(int page, int size) {
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("page", page);
        queryParams.put("size", size);
        return apiClient.getPage(ApiEndpointConstants.UPLOADS_HISTORY, UploadHistoryDto.class, queryParams);
    }
}
