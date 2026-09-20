package com.crm.platform.customer.controller;

import com.crm.platform.common.dto.ApiResponse;
import com.crm.platform.common.dto.PageMetadata;
import com.crm.platform.customer.dto.CustomerPatchRequestDto;
import com.crm.platform.customer.dto.CustomerRequestDto;
import com.crm.platform.customer.dto.CustomerResponseDto;
import com.crm.platform.customer.dto.CustomerSearchCriteria;
import com.crm.platform.customer.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerResponseDto>> createCustomer(
            @Valid @RequestBody CustomerRequestDto requestDto) {
        CustomerResponseDto created = customerService.createCustomer(requestDto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(ApiResponse.success(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponseDto>> getCustomerById(@PathVariable Long id) {
        CustomerResponseDto customer = customerService.getCustomerById(id);
        return ResponseEntity.ok(ApiResponse.success(customer));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponseDto>> patchCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CustomerPatchRequestDto patchDto) {
        CustomerResponseDto updated = customerService.patchCustomer(id, patchDto);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerResponseDto>>> searchCustomers(
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String tag,
            @PageableDefault(page = 0, size = 20) Pageable pageable) {

        CustomerSearchCriteria criteria = new CustomerSearchCriteria(
                firstName, lastName, email, city, country, tag
        );

        Page<CustomerResponseDto> pageResult = customerService.searchCustomers(criteria, pageable);
        PageMetadata pagination = PageMetadata.fromPage(pageResult);

        return ResponseEntity.ok(ApiResponse.success(pageResult.getContent(), pagination));
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> countActiveCustomers() {
        long count = customerService.countActiveCustomers();
        Map<String, Long> data = Collections.singletonMap("totalActiveCustomers", count);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
