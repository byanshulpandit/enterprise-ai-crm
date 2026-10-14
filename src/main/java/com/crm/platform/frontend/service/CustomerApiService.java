package com.crm.platform.frontend.service;

import com.crm.platform.customer.dto.CustomerPatchRequestDto;
import com.crm.platform.customer.dto.CustomerRequestDto;
import com.crm.platform.customer.dto.CustomerResponseDto;
import com.crm.platform.frontend.client.ApiEndpointConstants;
import com.crm.platform.frontend.client.BackendApiClient;
import com.crm.platform.frontend.client.PageResult;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service invoking backend Customer REST endpoints.
 */
@Service
public class CustomerApiService {

    private final BackendApiClient apiClient;

    public CustomerApiService(BackendApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public PageResult<CustomerResponseDto> searchCustomers(
            String firstName, String lastName, String email,
            String city, String country, String tag,
            int page, int size, String sort) {

        Map<String, Object> queryParams = new HashMap<>();
        if (firstName != null && !firstName.isBlank()) queryParams.put("firstName", firstName);
        if (lastName != null && !lastName.isBlank()) queryParams.put("lastName", lastName);
        if (email != null && !email.isBlank()) queryParams.put("email", email);
        if (city != null && !city.isBlank()) queryParams.put("city", city);
        if (country != null && !country.isBlank()) queryParams.put("country", country);
        if (tag != null && !tag.isBlank()) queryParams.put("tag", tag);
        queryParams.put("page", page);
        queryParams.put("size", size);
        if (sort != null && !sort.isBlank()) queryParams.put("sort", sort);

        return apiClient.getPage(ApiEndpointConstants.CUSTOMERS, CustomerResponseDto.class, queryParams);
    }

    public CustomerResponseDto getCustomerById(Long id) {
        return apiClient.get(ApiEndpointConstants.CUSTOMER_BY_ID, CustomerResponseDto.class, id);
    }

    public CustomerResponseDto createCustomer(CustomerRequestDto requestDto) {
        return apiClient.post(ApiEndpointConstants.CUSTOMERS, requestDto, CustomerResponseDto.class);
    }

    public CustomerResponseDto patchCustomer(Long id, CustomerPatchRequestDto patchDto) {
        return apiClient.patch(ApiEndpointConstants.CUSTOMER_BY_ID, patchDto, CustomerResponseDto.class, id);
    }

    public void deleteCustomer(Long id) {
        apiClient.delete(ApiEndpointConstants.CUSTOMER_BY_ID, id);
    }

    public long countActiveCustomers() {
        try {
            JsonNode response = apiClient.get(ApiEndpointConstants.CUSTOMERS + "/count", JsonNode.class);
            if (response != null && response.has("totalActiveCustomers")) {
                return response.get("totalActiveCustomers").asLong(0L);
            }
        } catch (Exception ignored) {
        }
        return 0L;
    }
}
