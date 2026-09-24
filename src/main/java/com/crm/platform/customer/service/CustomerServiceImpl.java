package com.crm.platform.customer.service;

import com.crm.platform.common.exception.DuplicateResourceException;
import com.crm.platform.common.exception.InvalidRequestException;
import com.crm.platform.common.exception.ResourceNotFoundException;
import com.crm.platform.customer.dto.CustomerPatchRequestDto;
import com.crm.platform.customer.dto.CustomerRequestDto;
import com.crm.platform.customer.dto.CustomerResponseDto;
import com.crm.platform.customer.dto.CustomerSearchCriteria;
import com.crm.platform.customer.entity.Customer;
import com.crm.platform.customer.mapper.CustomerMapper;
import com.crm.platform.customer.repository.CustomerRepository;
import com.crm.platform.customer.repository.CustomerSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt",
            "lastName",
            "totalSpend"
    );

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    public CustomerServiceImpl(CustomerRepository customerRepository, CustomerMapper customerMapper) {
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
    }

    @Override
    public CustomerResponseDto createCustomer(CustomerRequestDto requestDto) {
        String email = requestDto.getEmail().trim().toLowerCase();

        // Enforce email uniqueness at application layer before DB constraint check
        Optional<Customer> existingCustomer = customerRepository.findByEmail(email);
        if (existingCustomer.isPresent()) {
            if (existingCustomer.get().getDeletedAt() != null) {
                throw new DuplicateResourceException("Customer with email already exists (soft-deleted): " + email);
            } else {
                throw new DuplicateResourceException("A customer with email '" + email + "' already exists.");
            }
        }

        Customer customer = customerMapper.toEntity(requestDto);
        Customer savedCustomer = customerRepository.save(customer);
        return customerMapper.toDto(savedCustomer);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponseDto getCustomerById(Long id) {
        Customer customer = customerRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
        return customerMapper.toDto(customer);
    }

    @Override
    public CustomerResponseDto patchCustomer(Long id, CustomerPatchRequestDto patchDto) {
        Customer customer = customerRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));

        // If email update is requested, verify uniqueness against other records
        if (patchDto.getEmail() != null && !patchDto.getEmail().trim().isEmpty()) {
            String newEmail = patchDto.getEmail().trim().toLowerCase();
            Optional<Customer> existingWithEmail = customerRepository.findByEmail(newEmail);
            if (existingWithEmail.isPresent() && !existingWithEmail.get().getId().equals(id)) {
                if (existingWithEmail.get().getDeletedAt() != null) {
                    throw new DuplicateResourceException("Customer with email already exists (soft-deleted): " + newEmail);
                } else {
                    throw new DuplicateResourceException("A customer with email '" + newEmail + "' already exists.");
                }
            }
        }

        boolean modified = customerMapper.applyPatch(customer, patchDto);
        if (modified) {
            customer.setUpdatedAt(java.time.Instant.now());
            customer = customerRepository.saveAndFlush(customer);
        }

        return customerMapper.toDto(customer);
    }

    @Override
    public void deleteCustomer(Long id) {
        Customer customer = customerRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));

        // Soft delete: sets deleted_at and status = DELETED, physically preserving the row
        customer.softDelete();
        customerRepository.save(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerResponseDto> searchCustomers(CustomerSearchCriteria criteria, Pageable pageable) {
        Pageable effectivePageable = validateAndBuildPageable(pageable);
        Specification<Customer> spec = CustomerSpecification.buildSpecification(criteria);

        Page<Customer> page = customerRepository.findAll(spec, effectivePageable);
        return page.map(customerMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveCustomers() {
        return customerRepository.countByDeletedAtIsNull();
    }

    private Pageable validateAndBuildPageable(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        Sort sort = pageable.getSort();
        if (sort.isSorted()) {
            for (Sort.Order order : sort) {
                if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                    throw new InvalidRequestException(
                            "Invalid sort field: '" + order.getProperty() + "'. Allowed sort fields are: " +
                                    String.join(", ", ALLOWED_SORT_FIELDS)
                    );
                }
            }
        } else {
            // Default sort = createdAt descending
            sort = Sort.by(Sort.Direction.DESC, "createdAt");
        }

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }
}
