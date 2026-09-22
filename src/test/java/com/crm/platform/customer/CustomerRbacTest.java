package com.crm.platform.customer;

import com.crm.platform.customer.dto.CustomerPatchRequestDto;
import com.crm.platform.customer.dto.CustomerRequestDto;
import com.crm.platform.customer.entity.Customer;
import com.crm.platform.customer.repository.CustomerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class CustomerRbacTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    private Customer existingCustomer;

    @BeforeEach
    void setUp() {
        customerRepository.deleteAll();
        Customer customer = new Customer();
        customer.setFirstName("Jane");
        customer.setLastName("Doe");
        customer.setEmail("jane.doe@example.com");
        customer.setPhone("+1234567890");
        customer.setCity("NYC");
        customer.setCountry("USA");
        customer.setTotalSpend(new BigDecimal("1000.00"));
        customer.setVisitCount(2);
        customer.addTag("VIP");
        existingCustomer = customerRepository.save(customer);
    }

    @Test
    @DisplayName("1. Unauthenticated request to customer endpoints returns 401 Unauthorized")
    void testUnauthenticatedAccessReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("2. MARKETER can create customer")
    @WithMockUser(roles = "MARKETER")
    void testMarketerCanCreateCustomer() throws Exception {
        CustomerRequestDto request = new CustomerRequestDto(
                "Alice", "Smith", "alice@example.com", "+1987654321", "LA", "USA",
                new BigDecimal("500.00"), 1, LocalDate.of(2026, 1, 1), Set.of("TECH")
        );

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.email", is("alice@example.com")));
    }

    @Test
    @DisplayName("3. MARKETER can read customers")
    @WithMockUser(roles = "MARKETER")
    void testMarketerCanReadCustomers() throws Exception {
        mockMvc.perform(get("/api/v1/customers/" + existingCustomer.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(existingCustomer.getId().intValue())));
    }

    @Test
    @DisplayName("4. MARKETER can patch customer")
    @WithMockUser(roles = "MARKETER")
    void testMarketerCanPatchCustomer() throws Exception {
        CustomerPatchRequestDto patchDto = new CustomerPatchRequestDto();
        patchDto.setCity("San Francisco");

        mockMvc.perform(patch("/api/v1/customers/" + existingCustomer.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.city", is("San Francisco")));
    }

    @Test
    @DisplayName("5. MARKETER cannot delete customer: returns 403 Forbidden")
    @WithMockUser(roles = "MARKETER")
    void testMarketerCannotDeleteCustomer() throws Exception {
        mockMvc.perform(delete("/api/v1/customers/" + existingCustomer.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("FORBIDDEN")));
    }

    @Test
    @DisplayName("6. ADMIN can delete customer: returns 204 No Content")
    @WithMockUser(roles = "ADMIN")
    void testAdminCanDeleteCustomer() throws Exception {
        mockMvc.perform(delete("/api/v1/customers/" + existingCustomer.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("7. ADMIN can perform all customer operations")
    @WithMockUser(roles = "ADMIN")
    void testAdminCanPerformCustomerOperations() throws Exception {
        mockMvc.perform(get("/api/v1/customers/" + existingCustomer.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }
}
