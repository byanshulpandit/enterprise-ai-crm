package com.crm.platform.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private boolean success = false;
    private ErrorDetails error;

    public ErrorResponse() {
    }

    public ErrorResponse(ErrorDetails error) {
        this.success = false;
        this.error = error;
    }

    public static ErrorResponse of(String code, String message, List<ValidationErrorDetail> details) {
        ErrorDetails errorDetails = new ErrorDetails(
                code,
                message,
                Instant.now().toString(),
                UUID.randomUUID().toString(),
                details
        );
        return new ErrorResponse(errorDetails);
    }

    public static ErrorResponse of(String code, String message) {
        return of(code, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public ErrorDetails getError() {
        return error;
    }

    public void setError(ErrorDetails error) {
        this.error = error;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorDetails {
        private String code;
        private String message;
        private String timestamp;
        private String requestId;
        private List<ValidationErrorDetail> details;

        public ErrorDetails() {
        }

        public ErrorDetails(String code, String message, String timestamp, String requestId, List<ValidationErrorDetail> details) {
            this.code = code;
            this.message = message;
            this.timestamp = timestamp;
            this.requestId = requestId;
            this.details = details;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
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

        public List<ValidationErrorDetail> getDetails() {
            return details;
        }

        public void setDetails(List<ValidationErrorDetail> details) {
            this.details = details;
        }
    }
}
