package com.crm.platform.customer.repository;

import com.crm.platform.customer.dto.CustomerSearchCriteria;
import com.crm.platform.customer.entity.Customer;
import com.crm.platform.customer.entity.CustomerTag;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class CustomerSpecification {

    public static Specification<Customer> buildSpecification(CustomerSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Invariant: Always exclude soft-deleted records
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (criteria == null) {
                return cb.and(predicates.toArray(new Predicate[0]));
            }

            if (criteria.getFirstName() != null && !criteria.getFirstName().trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("firstName")), "%" + criteria.getFirstName().trim().toLowerCase() + "%"));
            }

            if (criteria.getLastName() != null && !criteria.getLastName().trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("lastName")), "%" + criteria.getLastName().trim().toLowerCase() + "%"));
            }

            if (criteria.getEmail() != null && !criteria.getEmail().trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("email")), "%" + criteria.getEmail().trim().toLowerCase() + "%"));
            }

            if (criteria.getCity() != null && !criteria.getCity().trim().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("city")), criteria.getCity().trim().toLowerCase()));
            }

            if (criteria.getCountry() != null && !criteria.getCountry().trim().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("country")), criteria.getCountry().trim().toLowerCase()));
            }

            if (criteria.getTag() != null && !criteria.getTag().trim().isEmpty()) {
                // Distinct to prevent duplicates when joining tags
                if (query != null) {
                    query.distinct(true);
                }
                Join<Customer, CustomerTag> tagsJoin = root.join("tags", JoinType.INNER);
                predicates.add(cb.equal(cb.lower(tagsJoin.get("tag")), criteria.getTag().trim().toLowerCase()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
