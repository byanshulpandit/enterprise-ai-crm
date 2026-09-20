package com.crm.platform.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private T data;
    private Metadata metadata;

    public ApiResponse() {
    }

    public ApiResponse(boolean success, T data, Metadata metadata) {
        this.success = success;
        this.data = data;
        this.metadata = metadata;
    }

    public static <T> ApiResponse<T> success(T data) {
        Metadata meta = new Metadata(Instant.now().toString(), UUID.randomUUID().toString(), null);
        return new ApiResponse<>(true, data, meta);
    }

    public static <T> ApiResponse<T> success(T data, PageMetadata pagination) {
        Metadata meta = new Metadata(Instant.now().toString(), UUID.randomUUID().toString(), pagination);
        return new ApiResponse<>(true, data, meta);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Metadata {
        private String timestamp;
        private String requestId;
        private PageMetadata pagination;

        public Metadata() {
        }

        public Metadata(String timestamp, String requestId, PageMetadata pagination) {
            this.timestamp = timestamp;
            this.requestId = requestId;
            this.pagination = pagination;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public String getRequestId() {
            return requestId;
        }

        public void setRequestId(String requestId) {
            this.requestId = requestId;
        }

        public PageMetadata getPagination() {
            return pagination;
        }

        public void setPagination(PageMetadata pagination) {
            this.pagination = pagination;
        }
    }
}
