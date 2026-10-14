package com.crm.platform.frontend.client;

import java.util.Collections;
import java.util.List;

/**
 * Structured exception encapsulating backend ErrorResponse payload and HTTP status.
 */
public class ApiException extends RuntimeException {

    private final int statusCode;
    private final String errorCode;
    private final List<String> fieldErrors;

    public ApiException(int statusCode, String message) {
        this(statusCode, "ERR_GENERIC", message, Collections.emptyList());
    }

    public ApiException(int statusCode, String errorCode, String message) {
        this(statusCode, errorCode, message, Collections.emptyList());
    }

    public ApiException(int statusCode, String errorCode, String message, List<String> fieldErrors) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode != null ? errorCode : "ERR_UNKNOWN";
        this.fieldErrors = fieldErrors != null ? fieldErrors : Collections.emptyList();
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public List<String> getFieldErrors() {
        return fieldErrors;
    }

    public String getUserFriendlyMessage() {
        if (!fieldErrors.isEmpty()) {
            return getMessage() + ": " + String.join(", ", fieldErrors);
        }
        return getMessage();
    }
}
