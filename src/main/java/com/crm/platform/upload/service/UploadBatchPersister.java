package com.crm.platform.upload.service;

import com.crm.platform.customer.entity.Customer;
import com.crm.platform.customer.repository.CustomerRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class UploadBatchPersister {

    private final CustomerRepository customerRepository;

    public UploadBatchPersister(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    /**
     * Persists a batch of customers in an isolated new transaction.
     * If any database constraint violation occurs, this transaction is rolled back completely,
     * leaving the database clean and allowing fallback per-row persistence.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Customer> persistBatch(List<Customer> customers) {
        List<Customer> saved = customerRepository.saveAll(customers);
        customerRepository.flush();
        return saved;
    }

    /**
     * Persists an individual customer in its own isolated new transaction.
     * If this single record fails, only this individual transaction rolls back,
     * without poisoning the session or affecting preceding/subsequent records.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Customer persistSingle(Customer customer) {
        return customerRepository.saveAndFlush(customer);
    }
}
