package com.crm.platform.customer.repository;

import com.crm.platform.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"tags"})
    Optional<Customer> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByEmailAndDeletedAtIsNull(String email);

    boolean existsByEmail(String email);

    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByEmailAndDeletedAtIsNull(String email);

    long countByDeletedAtIsNull();
}
