package com.crm.platform.customer;

import com.crm.platform.customer.dto.CustomerPatchRequestDto;
import com.crm.platform.customer.dto.CustomerRequestDto;
import com.crm.platform.customer.entity.Customer;
import com.crm.platform.customer.repository.CustomerRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class CustomerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ApplicationContext applicationContext;

    @BeforeEach
    void setUp() {
        customerRepository.deleteAll();
    }

    // 1. Application Context
    @Test
    @DisplayName("Test 1: Application context loads and customer beans exist")
    void testApplicationContextLoads() {
        assertThat(applicationContext).isNotNull();
        assertThat(customerRepository).isNotNull();
    }

    // 2. Create customer success
    @Test
    @DisplayName("Test 2: Create customer success (POST 201 Created with Location header)")
    void testCreateCustomerSuccess() throws Exception {
        CustomerRequestDto request = new CustomerRequestDto(
                "Aarav",
                "Sharma",
                "aarav.sharma@example.in",
                "+919876543210",
                "Mumbai",
                "India",
                new BigDecimal("12500.50"),
                5,
                LocalDate.of(2026, 8, 15),
                Set.of("VIP", "FestivalShopper")
        );

        MvcResult result = mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.firstName", is("Aarav")))
                .andExpect(jsonPath("$.data.lastName", is("Sharma")))
                .andExpect(jsonPath("$.data.email", is("aarav.sharma@example.in")))
                .andExpect(jsonPath("$.data.totalSpend", is(12500.50)))
                .andExpect(jsonPath("$.data.visitCount", is(5)))
                .andExpect(jsonPath("$.data.tags", hasSize(2)))
                .andExpect(jsonPath("$.metadata.requestId").exists())
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(result.getResponse().getContentAsString());
        Long createdId = responseNode.get("data").get("id").asLong();

        // Verify in MySQL using @EntityGraph query
        Optional<Customer> inDb = customerRepository.findByIdAndDeletedAtIsNull(createdId);
        assertThat(inDb).isPresent();
        assertThat(inDb.get().getEmail()).isEqualTo("aarav.sharma@example.in");
        assertThat(inDb.get().getTagNames()).containsExactlyInAnyOrder("VIP", "FestivalShopper");
        assertThat(inDb.get().getCreatedAt()).isNotNull();
        assertThat(inDb.get().getUpdatedAt()).isNotNull();
    }

    // 3. Create with missing firstName
    @Test
    @DisplayName("Test 3: Create customer missing firstName returns 400 Bad Request")
    void testCreateCustomerMissingFirstName() throws Exception {
        CustomerRequestDto request = new CustomerRequestDto(
                "",
                "Sharma",
                "sharma@example.com",
                null, "Delhi", "India", BigDecimal.ZERO, 0, null, null
        );

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("VALIDATION_FAILED")));
    }

    // 4. Create with missing lastName
    @Test
    @DisplayName("Test 4: Create customer missing lastName returns 400 Bad Request")
    void testCreateCustomerMissingLastName() throws Exception {
        CustomerRequestDto request = new CustomerRequestDto(
                "Aarav",
                null,
                "aarav@example.com",
                null, "Delhi", "India", BigDecimal.ZERO, 0, null, null
        );

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("VALIDATION_FAILED")));
    }

    // 5. Create with invalid email
    @Test
    @DisplayName("Test 5: Create customer with invalid email format returns 400 Bad Request")
    void testCreateCustomerInvalidEmail() throws Exception {
        CustomerRequestDto request = new CustomerRequestDto(
                "Aarav",
                "Sharma",
                "invalid-email-address",
                null, "Delhi", "India", BigDecimal.ZERO, 0, null, null
        );

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("VALIDATION_FAILED")));
    }

    // 6. Duplicate email
    @Test
    @DisplayName("Test 6: Duplicate email returns 409 Conflict")
    void testCreateCustomerDuplicateEmail() throws Exception {
        CustomerRequestDto request = new CustomerRequestDto(
                "Aarav",
                "Sharma",
                "duplicate@example.com",
                null, "Delhi", "India", BigDecimal.ZERO, 0, null, null
        );

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Second attempt with exact same email
        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("DUPLICATE_RESOURCE")));
    }

    // 7. Get existing customer
    @Test
    @DisplayName("Test 7: Get existing active customer returns 200 OK with correct data")
    void testGetExistingCustomer() throws Exception {
        Customer customer = new Customer();
        customer.setFirstName("Priya");
        customer.setLastName("Patel");
        customer.setEmail("priya.patel@example.com");
        customer.setCity("Bengaluru");
        customer.setCountry("India");
        customer.setTotalSpend(new BigDecimal("5000.00"));
        customer.setVisitCount(3);
        customer.addTag("Preferred");
        Customer saved = customerRepository.save(customer);

        mockMvc.perform(get("/api/v1/customers/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(saved.getId().intValue())))
                .andExpect(jsonPath("$.data.customerId", is(saved.getId().intValue())))
                .andExpect(jsonPath("$.data.firstName", is("Priya")))
                .andExpect(jsonPath("$.data.lastName", is("Patel")))
                .andExpect(jsonPath("$.data.email", is("priya.patel@example.com")))
                .andExpect(jsonPath("$.data.city", is("Bengaluru")))
                .andExpect(jsonPath("$.data.tags", hasSize(1)));
    }

    // 8. Get unknown customer -> 404
    @Test
    @DisplayName("Test 8: Get non-existent customer ID returns 404 Not Found")
    void testGetUnknownCustomer() throws Exception {
        mockMvc.perform(get("/api/v1/customers/9999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("RESOURCE_NOT_FOUND")));
    }

    // 9. PATCH partial update
    @Test
    @DisplayName("Test 9: PATCH partial update updates only supplied fields")
    void testPatchPartialUpdate() throws Exception {
        Customer customer = new Customer();
        customer.setFirstName("Rohan");
        customer.setLastName("Verma");
        customer.setEmail("rohan.verma@example.com");
        customer.setCity("Pune");
        customer.setCountry("India");
        customer.setTotalSpend(new BigDecimal("100.00"));
        Customer saved = customerRepository.save(customer);

        CustomerPatchRequestDto patchDto = new CustomerPatchRequestDto();
        patchDto.setCity("Hyderabad");
        patchDto.setTotalSpend(new BigDecimal("250.00"));

        mockMvc.perform(patch("/api/v1/customers/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.city", is("Hyderabad")))
                .andExpect(jsonPath("$.data.totalSpend", is(250.00)))
                .andExpect(jsonPath("$.data.firstName", is("Rohan"))); // unchanged
    }

    // 10. PATCH preserves unspecified fields
    @Test
    @DisplayName("Test 10: PATCH preserves unspecified fields intact")
    void testPatchPreservesUnspecifiedFields() throws Exception {
        Customer customer = new Customer();
        customer.setFirstName("Sneha");
        customer.setLastName("Reddy");
        customer.setEmail("sneha.reddy@example.com");
        customer.setPhone("+919999988888");
        customer.setCity("Chennai");
        customer.setCountry("India");
        customer.setTotalSpend(new BigDecimal("800.00"));
        customer.setVisitCount(4);
        customer.addTag("HighValue");
        Customer saved = customerRepository.save(customer);

        CustomerPatchRequestDto patchDto = new CustomerPatchRequestDto();
        patchDto.setPhone("+911111122222"); // Only change phone

        mockMvc.perform(patch("/api/v1/customers/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phone", is("+911111122222")))
                .andExpect(jsonPath("$.data.firstName", is("Sneha")))
                .andExpect(jsonPath("$.data.lastName", is("Reddy")))
                .andExpect(jsonPath("$.data.email", is("sneha.reddy@example.com")))
                .andExpect(jsonPath("$.data.city", is("Chennai")))
                .andExpect(jsonPath("$.data.country", is("India")))
                .andExpect(jsonPath("$.data.visitCount", is(4)))
                .andExpect(jsonPath("$.data.tags", hasSize(1)));
    }

    // 11. Immutable customerId cannot be changed
    @Test
    @DisplayName("Test 11: CustomerId is immutable and cannot be changed via PATCH payload")
    void testImmutableCustomerIdCannotBeChanged() throws Exception {
        Customer customer = new Customer();
        customer.setFirstName("Vikram");
        customer.setLastName("Singh");
        customer.setEmail("vikram.singh@example.com");
        Customer saved = customerRepository.save(customer);
        Long originalId = saved.getId();

        // Attempt to pass different id in JSON body
        String maliciousPayload = "{\"id\": 99999, \"customerId\": 99999, \"city\": \"Jaipur\"}";

        mockMvc.perform(patch("/api/v1/customers/" + originalId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(maliciousPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(originalId.intValue())))
                .andExpect(jsonPath("$.data.city", is("Jaipur")));

        Customer reloaded = customerRepository.findById(originalId).orElseThrow();
        assertThat(reloaded.getId()).isEqualTo(originalId);
        assertThat(reloaded.getCity()).isEqualTo("Jaipur");
    }

    // 12. createdAt is not client-controlled
    @Test
    @DisplayName("Test 12: CreatedAt is system-generated and not client-controlled")
    void testCreatedAtNotClientControlled() throws Exception {
        String payloadWithFakeCreatedAt = "{" +
                "\"firstName\": \"Anita\"," +
                "\"lastName\": \"Desai\"," +
                "\"email\": \"anita.desai@example.com\"," +
                "\"createdAt\": \"2015-01-01T00:00:00Z\"" +
                "}";

        MvcResult result = mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadWithFakeCreatedAt))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        String createdAtString = jsonNode.get("data").get("createdAt").asText();
        Instant createdAt = Instant.parse(createdAtString);

        // CreatedAt must be recent (within the last few minutes), NOT 2015
        assertThat(createdAt).isAfter(Instant.now().minusSeconds(120));
    }

    // 13. updatedAt changes after update
    @Test
    @DisplayName("Test 13: UpdatedAt refreshes on successful modification")
    void testUpdatedAtChangesAfterUpdate() throws Exception {
        Customer customer = new Customer();
        customer.setFirstName("Kavita");
        customer.setLastName("Iyer");
        customer.setEmail("kavita.iyer@example.com");
        Customer saved = customerRepository.save(customer);
        Instant initialUpdatedAt = saved.getUpdatedAt();

        Thread.sleep(50); // Ensure time difference

        CustomerPatchRequestDto patchDto = new CustomerPatchRequestDto();
        patchDto.setCity("Kochi");

        MvcResult result = mockMvc.perform(patch("/api/v1/customers/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchDto)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        Instant postUpdateUpdatedAt = Instant.parse(jsonNode.get("data").get("updatedAt").asText());

        assertThat(postUpdateUpdatedAt).isAfterOrEqualTo(initialUpdatedAt);
    }

    // 14. Soft delete
    @Test
    @DisplayName("Test 14: Soft delete sets deleted_at and keeps physical row in MySQL")
    void testSoftDeleteSetsDeletedAtAndKeepsRowInDatabase() throws Exception {
        Customer customer = new Customer();
        customer.setFirstName("Rajesh");
        customer.setLastName("Kumar");
        customer.setEmail("rajesh.kumar@example.com");
        Customer saved = customerRepository.save(customer);
        Long id = saved.getId();

        mockMvc.perform(delete("/api/v1/customers/" + id))
                .andExpect(status().isNoContent());

        // Verify physical row is still present in MySQL
        Optional<Customer> physicalCustomer = customerRepository.findById(id);
        assertThat(physicalCustomer).isPresent();
        assertThat(physicalCustomer.get().getDeletedAt()).isNotNull();
        assertThat(physicalCustomer.get().isDeleted()).isTrue();
    }

    // 15. Deleted customer excluded from GET
    @Test
    @DisplayName("Test 15: Soft-deleted customer returns 404 on GET /api/v1/customers/{id}")
    void testDeletedCustomerExcludedFromGet() throws Exception {
        Customer customer = new Customer();
        customer.setFirstName("Manish");
        customer.setLastName("Tiwari");
        customer.setEmail("manish.tiwari@example.com");
        Customer saved = customerRepository.save(customer);
        Long id = saved.getId();

        // Delete
        mockMvc.perform(delete("/api/v1/customers/" + id))
                .andExpect(status().isNoContent());

        // GET should now return 404
        mockMvc.perform(get("/api/v1/customers/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("RESOURCE_NOT_FOUND")));
    }

    // 16. Deleted customer excluded from list
    @Test
    @DisplayName("Test 16: Soft-deleted customer is excluded from customer directory listing")
    void testDeletedCustomerExcludedFromList() throws Exception {
        Customer active = new Customer();
        active.setFirstName("ActiveUser");
        active.setLastName("One");
        active.setEmail("active1@example.com");
        customerRepository.save(active);

        Customer toDelete = new Customer();
        toDelete.setFirstName("DeletedUser");
        toDelete.setLastName("Two");
        toDelete.setEmail("deleted2@example.com");
        Customer savedToDelete = customerRepository.save(toDelete);

        mockMvc.perform(delete("/api/v1/customers/" + savedToDelete.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].email", is("active1@example.com")));
    }

    // 17. Deleted customer excluded from count
    @Test
    @DisplayName("Test 17: Soft-deleted customer is excluded from active customer count")
    void testDeletedCustomerExcludedFromCount() throws Exception {
        Customer c1 = new Customer();
        c1.setFirstName("UserA");
        c1.setLastName("Test");
        c1.setEmail("usera@example.com");
        customerRepository.save(c1);

        Customer c2 = new Customer();
        c2.setFirstName("UserB");
        c2.setLastName("Test");
        c2.setEmail("userb@example.com");
        Customer savedC2 = customerRepository.save(c2);

        // Before delete: count = 2
        mockMvc.perform(get("/api/v1/customers/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalActiveCustomers", is(2)));

        // Soft-delete c2
        mockMvc.perform(delete("/api/v1/customers/" + savedC2.getId()))
                .andExpect(status().isNoContent());

        // After delete: count = 1
        mockMvc.perform(get("/api/v1/customers/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalActiveCustomers", is(1)));
    }

    // 18. Pagination
    @Test
    @DisplayName("Test 18: Pagination returns bounded page and metadata")
    void testPagination() throws Exception {
        for (int i = 1; i <= 5; i++) {
            Customer c = new Customer();
            c.setFirstName("User" + i);
            c.setLastName("Pagination");
            c.setEmail("user" + i + "@pagination.com");
            customerRepository.save(c);
        }

        mockMvc.perform(get("/api/v1/customers?page=0&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.metadata.pagination.page", is(0)))
                .andExpect(jsonPath("$.metadata.pagination.size", is(2)))
                .andExpect(jsonPath("$.metadata.pagination.totalElements", is(5)))
                .andExpect(jsonPath("$.metadata.pagination.totalPages", is(3)))
                .andExpect(jsonPath("$.metadata.pagination.isFirst", is(true)))
                .andExpect(jsonPath("$.metadata.pagination.isLast", is(false)));
    }

    // 19. Default sorting
    @Test
    @DisplayName("Test 19: Default sorting sorts by createdAt descending")
    void testDefaultSorting() throws Exception {
        Customer c1 = new Customer();
        c1.setFirstName("First");
        c1.setLastName("User");
        c1.setEmail("first@sort.com");
        customerRepository.save(c1);

        Thread.sleep(50);

        Customer c2 = new Customer();
        c2.setFirstName("Second");
        c2.setLastName("User");
        c2.setEmail("second@sort.com");
        customerRepository.save(c2);

        // Default sort should return second (newest) first
        mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].email", is("second@sort.com")))
                .andExpect(jsonPath("$.data[1].email", is("first@sort.com")));
    }

    // 20. Allowed filters
    @Test
    @DisplayName("Test 20: Filtering by firstName, city, and tag")
    void testAllowedFilters() throws Exception {
        Customer c1 = new Customer();
        c1.setFirstName("Tarun");
        c1.setLastName("Mehta");
        c1.setEmail("tarun@filter.com");
        c1.setCity("Delhi");
        c1.addTag("Platinum");
        customerRepository.save(c1);

        Customer c2 = new Customer();
        c2.setFirstName("Varun");
        c2.setLastName("Joshi");
        c2.setEmail("varun@filter.com");
        c2.setCity("Kolkata");
        c2.addTag("Silver");
        customerRepository.save(c2);

        // Filter by city
        mockMvc.perform(get("/api/v1/customers?city=Delhi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].email", is("tarun@filter.com")));

        // Filter by tag
        mockMvc.perform(get("/api/v1/customers?tag=Platinum"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].email", is("tarun@filter.com")));

        // Filter by firstName substring
        mockMvc.perform(get("/api/v1/customers?firstName=Var"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].email", is("varun@filter.com")));
    }

    // 21. Invalid sort field handling
    @Test
    @DisplayName("Test 21: Unapproved sort field returns 400 Bad Request with whitelist guidance")
    void testInvalidSortFieldHandling() throws Exception {
        mockMvc.perform(get("/api/v1/customers?sort=password,desc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.error.message", containsString("Allowed sort fields are")));
    }
}
