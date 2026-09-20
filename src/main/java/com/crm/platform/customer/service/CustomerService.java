package com.crm.platform.customer.service;

import com.crm.platform.customer.dto.CustomerPatchRequestDto;
import com.crm.platform.customer.dto.CustomerRequestDto;
import com.crm.platform.customer.dto.CustomerResponseDto;
import com.crm.platform.customer.dto.CustomerSearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomerService {

    CustomerResponseDto createCustomer(CustomerRequestDto requestDto);

    CustomerResponseDto getCustomerById(Long id);

    CustomerResponseDto patchCustomer(Long id, CustomerPatchRequestDto patchDto);

    void deleteCustomer(Long id);

    Page<CustomerResponseDto> searchCustomers(CustomerSearchCriteria criteria, Pageable pageable);

    long countActiveCustomers();
}
