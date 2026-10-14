package com.crm.platform.frontend;

import com.crm.platform.frontend.client.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("ApiException Unit Tests")
class ApiExceptionTest {

    @Test
    @DisplayName("Should preserve HTTP status code and message")
    void shouldPreserveHttpStatusAndMessage() {
        ApiException ex = new ApiException(404, "Customer not found");
        assertEquals(404, ex.getStatusCode());
        assertEquals("Customer not found", ex.getMessage());

        ApiException authEx = new ApiException(401, "JWT token expired");
        assertEquals(401, authEx.getStatusCode());
        assertEquals("JWT token expired", authEx.getMessage());

        ApiException forbiddenEx = new ApiException(403, "Access Denied");
        assertEquals(403, forbiddenEx.getStatusCode());
        assertEquals("Access Denied", forbiddenEx.getMessage());
    }
}
