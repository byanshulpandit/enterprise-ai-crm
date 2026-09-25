package com.crm.platform.upload.service;

import com.crm.platform.customer.entity.Customer;
import com.crm.platform.customer.repository.CustomerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class UploadBatchPersisterIntegrationTest {

    @Autowired
    private UploadBatchPersister batchPersister;

    @Autowired
    private CustomerRepository customerRepository;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        customerRepository.deleteAll();
    }

    private Customer createCustomer(String email, String first, String last) {
        Customer c = new Customer();
        c.setEmail(email);
        c.setFirstName(first);
        c.setLastName(last);
        c.setTotalSpend(BigDecimal.valueOf(100.00));
        c.setVisitCount(2);
        return c;
    }

    @Test
    @DisplayName("persistBatch saves and flushes all valid customers in a clean transaction")
    void testPersistBatch_AllValid_Success() {
        List<Customer> batch = List.of(
                createCustomer("batch1@example.com", "John", "Doe"),
                createCustomer("batch2@example.com", "Jane", "Doe")
        );

        List<Customer> saved = batchPersister.persistBatch(batch);

        assertThat(saved).hasSize(2);
        assertThat(customerRepository.findByEmail("batch1@example.com")).isPresent();
        assertThat(customerRepository.findByEmail("batch2@example.com")).isPresent();
    }

    @Test
    @DisplayName("persistBatch rolls back entire batch on constraint violation, leaving database clean for per-row fallback")
    void testPersistBatch_ConstraintViolation_RollsBackEntireBatch() {
        // Pre-insert an existing customer
        customerRepository.saveAndFlush(createCustomer("existing@example.com", "Old", "User"));

        // Batch contains one valid customer and one duplicate that violates unique constraint
        List<Customer> batch = List.of(
                createCustomer("unique_in_batch@example.com", "Alice", "Wonderland"),
                createCustomer("existing@example.com", "Duplicate", "User")
        );

        assertThatThrownBy(() -> batchPersister.persistBatch(batch))
                .isInstanceOf(DataIntegrityViolationException.class);

        // Crucial invariant: The valid customer in the failed batch was NOT committed because the entire batch rolled back
        assertThat(customerRepository.findByEmail("unique_in_batch@example.com")).isEmpty();

        // Now test isolated per-row fallback: valid row persists, invalid row fails without poisoning session
        Customer validCustomer = createCustomer("unique_in_batch@example.com", "Alice", "Wonderland");
        Customer savedValid = batchPersister.persistSingle(validCustomer);
        assertThat(savedValid.getId()).isNotNull();
        assertThat(customerRepository.findByEmail("unique_in_batch@example.com")).isPresent();

        // Invalid row fails in its own isolated transaction
        Customer invalidCustomer = createCustomer("existing@example.com", "Duplicate", "User");
        assertThatThrownBy(() -> batchPersister.persistSingle(invalidCustomer))
                .isInstanceOf(DataIntegrityViolationException.class);

        // Following customer can still be persisted without session poisoning
        Customer subsequentCustomer = createCustomer("subsequent@example.com", "Charlie", "Brown");
        Customer savedSubsequent = batchPersister.persistSingle(subsequentCustomer);
        assertThat(savedSubsequent.getId()).isNotNull();
        assertThat(customerRepository.findByEmail("subsequent@example.com")).isPresent();
    }
}
