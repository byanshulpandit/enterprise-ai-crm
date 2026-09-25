package com.crm.platform.customer;

import com.crm.platform.common.exception.DuplicateResourceException;
import com.crm.platform.common.exception.ResourceNotFoundException;
import com.crm.platform.customer.dto.CustomerPatchRequestDto;
import com.crm.platform.customer.dto.CustomerRequestDto;
import com.crm.platform.customer.service.CustomerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = {"ADMIN", "MARKETER"})
public class CustomerValidationAndExceptionTest {


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustomerService customerService;

    // 1. Missing required firstName (null)
    @Test
    @DisplayName("Scenario 1: Missing required firstName (null) returns 400 VALIDATION_FAILED")
    void testMissingRequiredFirstName() throws Exception {
        CustomerRequestDto request = new CustomerRequestDto(
                null, "Sharma", "valid.email@example.com", null, "Delhi", "India",
                BigDecimal.ZERO, 0, null, null
        );

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.error.timestamp").exists())
                .andExpect(jsonPath("$.error.requestId").exists())
                .andExpect(jsonPath("$.error.details[0].field", is("firstName")))
                .andExpect(jsonPath("$.error.details[0].message", is("First name is required")));
    }

    // 2. Blank firstName ("   ")
    @Test
    @DisplayName("Scenario 2: Blank firstName (whitespace only) returns 400 VALIDATION_FAILED")
    void testBlankFirstName() throws Exception {
        CustomerRequestDto request = new CustomerRequestDto(
                "   ", "Sharma", "valid.email@example.com", null, "Delhi", "India",
                BigDecimal.ZERO, 0, null, null
        );

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.error.details[0].field", is("firstName")))
                .andExpect(jsonPath("$.error.details[0].message", is("First name is required")));
    }

    // 3. Invalid email format
    @Test
    @DisplayName("Scenario 3: Invalid email format returns 400 VALIDATION_FAILED")
    void testInvalidEmail() throws Exception {
        CustomerRequestDto request = new CustomerRequestDto(
                "Aarav", "Sharma", "invalid-email-format", null, "Delhi", "India",
                BigDecimal.ZERO, 0, null, null
        );

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.error.details[0].field", is("email")))
                .andExpect(jsonPath("$.error.details[0].message", is("Email must be a valid email address")));
    }

    // 4. Negative totalSpend
    @Test
    @DisplayName("Scenario 4: Negative totalSpend returns 400 VALIDATION_FAILED")
    void testNegativeTotalSpend() throws Exception {
        CustomerRequestDto request = new CustomerRequestDto(
                "Aarav", "Sharma", "aarav@example.com", null, "Delhi", "India",
                new BigDecimal("-50.00"), 0, null, null
        );

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.error.details[0].field", is("totalSpend")))
                .andExpect(jsonPath("$.error.details[0].message", is("Total spend must be greater than or equal to 0.00")));
    }

    // 5. Negative visitCount
    @Test
    @DisplayName("Scenario 5: Negative visitCount returns 400 VALIDATION_FAILED")
    void testNegativeVisitCount() throws Exception {
        CustomerRequestDto request = new CustomerRequestDto(
                "Aarav", "Sharma", "aarav@example.com", null, "Delhi", "India",
                BigDecimal.ZERO, -3, null, null
        );

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.error.details[0].field", is("visitCount")))
                .andExpect(jsonPath("$.error.details[0].message", is("Visit count cannot be negative")));
    }

    // 6. Invalid PATCH payload (blank firstName or negative spend)
    @Test
    @DisplayName("Scenario 6: Invalid PATCH payload (blank firstName) returns 400 VALIDATION_FAILED")
    void testInvalidPatchPayloadBlankFirstName() throws Exception {
        CustomerPatchRequestDto patch = new CustomerPatchRequestDto();
        patch.setFirstName("   ");

        mockMvc.perform(patch("/api/v1/customers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patch)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.error.details[0].field", is("firstName")))
                .andExpect(jsonPath("$.error.details[0].message", is("First name cannot be blank if provided")));
    }

    @Test
    @DisplayName("Scenario 6b: Invalid PATCH payload (negative totalSpend) returns 400 VALIDATION_FAILED")
    void testInvalidPatchPayloadNegativeSpend() throws Exception {
        CustomerPatchRequestDto patch = new CustomerPatchRequestDto();
        patch.setTotalSpend(new BigDecimal("-15.00"));

        mockMvc.perform(patch("/api/v1/customers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patch)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.error.details[0].field", is("totalSpend")))
                .andExpect(jsonPath("$.error.details[0].message", is("Total spend must be greater than or equal to 0.00")));
    }

    // 7. Malformed JSON
    @Test
    @DisplayName("Scenario 7: Malformed JSON syntax returns 400 MALFORMED_REQUEST")
    void testMalformedJson() throws Exception {
        String brokenJson = "{\"firstName\": \"Aarav\", \"email\": ";

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(brokenJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("MALFORMED_REQUEST")))
                .andExpect(jsonPath("$.error.message", is("Malformed or unreadable JSON request body")));
    }

    // 8. Invalid path variable / request parameter type
    @Test
    @DisplayName("Scenario 8: Non-numeric path variable returns 400 INVALID_PARAMETER")
    void testInvalidPathVariableType() throws Exception {
        mockMvc.perform(get("/api/v1/customers/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("INVALID_PARAMETER")))
                .andExpect(jsonPath("$.error.message", containsString("Parameter 'id' value 'not-a-number' is invalid. Required type: Long")));
    }

    // 9. Duplicate email
    @Test
    @DisplayName("Scenario 9: Duplicate email returns 409 DUPLICATE_RESOURCE")
    void testDuplicateEmail() throws Exception {
        when(customerService.createCustomer(any())).thenThrow(
                new DuplicateResourceException("A customer with email 'duplicate@example.com' already exists.")
        );

        CustomerRequestDto request = new CustomerRequestDto(
                "Aarav", "Sharma", "duplicate@example.com", null, "Delhi", "India",
                BigDecimal.ZERO, 0, null, null
        );

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("DUPLICATE_RESOURCE")))
                .andExpect(jsonPath("$.error.message", is("A customer with email 'duplicate@example.com' already exists.")));
    }

    // 10. Customer not found
    @Test
    @DisplayName("Scenario 10: Non-existent customer ID returns 404 RESOURCE_NOT_FOUND")
    void testCustomerNotFound() throws Exception {
        when(customerService.getCustomerById(99999L)).thenThrow(
                new ResourceNotFoundException("Customer not found with id: 99999")
        );

        mockMvc.perform(get("/api/v1/customers/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("RESOURCE_NOT_FOUND")))
                .andExpect(jsonPath("$.error.message", is("Customer not found with id: 99999")));
    }

    // 11. Generic unexpected exception fallback
    @Test
    @DisplayName("Scenario 11: Unexpected exception returns 500 INTERNAL_SERVER_ERROR without leaking internals")
    void testUnexpectedExceptionFallback() throws Exception {
        when(customerService.getCustomerById(12345L)).thenThrow(
                new RuntimeException("SQL syntax error or internal database crash at com.mysql.cj...")
        );

        mockMvc.perform(get("/api/v1/customers/12345"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("INTERNAL_SERVER_ERROR")))
                .andExpect(jsonPath("$.error.message", is("An unexpected internal server error occurred.")))
                .andExpect(jsonPath("$.error.timestamp").exists())
                .andExpect(jsonPath("$.error.requestId").exists())
                // Ensure internal stack traces and SQL details are masked
                .andExpect(jsonPath("$.error.message", not(containsString("SQL"))))
                .andExpect(jsonPath("$.error.message", not(containsString("com.mysql"))));
    }

    // 12. Unmapped route returns 404 RESOURCE_NOT_FOUND, not 500
    @Test
    @DisplayName("Scenario 12: Unmapped route returns 404 RESOURCE_NOT_FOUND (not 500)")
    void testUnmappedRouteReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/non-existent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("RESOURCE_NOT_FOUND")))
                .andExpect(jsonPath("$.error.message", containsString("non-existent")));
    }

    // 13. Unsupported HTTP method returns 405 METHOD_NOT_ALLOWED
    @Test
    @DisplayName("Scenario 13: Unsupported HTTP method (PUT) returns 405 METHOD_NOT_ALLOWED")
    void testUnsupportedMethod() throws Exception {
        mockMvc.perform(put("/api/v1/customers"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("METHOD_NOT_ALLOWED")))
                .andExpect(jsonPath("$.error.message", containsString("PUT")));
    }

    // 14. Unsupported Content-Type returns 415 UNSUPPORTED_MEDIA_TYPE
    @Test
    @DisplayName("Scenario 14: Unsupported Content-Type (text/plain) returns 415 UNSUPPORTED_MEDIA_TYPE")
    void testUnsupportedMediaType() throws Exception {
        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("hello world"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("UNSUPPORTED_MEDIA_TYPE")));
    }
}
