package com.crm.platform.customer.mapper;

import com.crm.platform.customer.dto.CustomerPatchRequestDto;
import com.crm.platform.customer.dto.CustomerRequestDto;
import com.crm.platform.customer.dto.CustomerResponseDto;
import com.crm.platform.customer.entity.Customer;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CustomerMapper {

    public Customer toEntity(CustomerRequestDto dto) {
        if (dto == null) {
            return null;
        }

        Customer customer = new Customer();
        customer.setFirstName(dto.getFirstName().trim());
        customer.setLastName(dto.getLastName().trim());
        customer.setEmail(dto.getEmail().trim().toLowerCase());
        customer.setPhone(dto.getPhone() != null ? dto.getPhone().trim() : null);
        customer.setCity(dto.getCity() != null ? dto.getCity().trim() : null);
        customer.setCountry(dto.getCountry() != null ? dto.getCountry().trim() : null);
        customer.setTotalSpend(dto.getTotalSpend() != null ? dto.getTotalSpend() : BigDecimal.ZERO);
        customer.setVisitCount(dto.getVisitCount() != null ? dto.getVisitCount() : 0);
        customer.setLastActiveDate(dto.getLastActiveDate());

        if (dto.getTags() != null) {
            customer.setTagsFromStrings(dto.getTags());
        }

        return customer;
    }

    public CustomerResponseDto toDto(Customer customer) {
        if (customer == null) {
            return null;
        }

        return new CustomerResponseDto(
                customer.getId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getCity(),
                customer.getCountry(),
                customer.getTotalSpend(),
                customer.getVisitCount(),
                customer.getLastActiveDate(),
                customer.getTagNames(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }

    public boolean applyPatch(Customer customer, CustomerPatchRequestDto patch) {
        boolean modified = false;

        if (patch.getFirstName() != null && !patch.getFirstName().trim().isEmpty()) {
            customer.setFirstName(patch.getFirstName().trim());
            modified = true;
        }
        if (patch.getLastName() != null && !patch.getLastName().trim().isEmpty()) {
            customer.setLastName(patch.getLastName().trim());
            modified = true;
        }
        if (patch.getEmail() != null && !patch.getEmail().trim().isEmpty()) {
            customer.setEmail(patch.getEmail().trim().toLowerCase());
            modified = true;
        }
        if (patch.getPhone() != null) {
            customer.setPhone(patch.getPhone().trim().isEmpty() ? null : patch.getPhone().trim());
            modified = true;
        }
        if (patch.getCity() != null) {
            customer.setCity(patch.getCity().trim().isEmpty() ? null : patch.getCity().trim());
            modified = true;
        }
        if (patch.getCountry() != null) {
            customer.setCountry(patch.getCountry().trim().isEmpty() ? null : patch.getCountry().trim());
            modified = true;
        }
        if (patch.getTotalSpend() != null) {
            customer.setTotalSpend(patch.getTotalSpend());
            modified = true;
        }
        if (patch.getVisitCount() != null) {
            customer.setVisitCount(patch.getVisitCount());
            modified = true;
        }
        if (patch.getLastActiveDate() != null) {
            customer.setLastActiveDate(patch.getLastActiveDate());
            modified = true;
        }
        if (patch.getTags() != null) {
            customer.setTagsFromStrings(patch.getTags());
            modified = true;
        }

        return modified;
    }
}
