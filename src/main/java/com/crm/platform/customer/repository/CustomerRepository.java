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

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(c.totalSpend), 0) FROM Customer c WHERE c.deletedAt IS NULL")
    java.math.BigDecimal sumActiveTotalSpend();

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(AVG(c.totalSpend), 0) FROM Customer c WHERE c.deletedAt IS NULL")
    java.math.BigDecimal avgActiveTotalSpend();

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(c.visitCount), 0) FROM Customer c WHERE c.deletedAt IS NULL")
    Long sumActiveVisitCount();

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(AVG(c.visitCount), 0.0) FROM Customer c WHERE c.deletedAt IS NULL")
    Double avgActiveVisitCount();

    @org.springframework.data.jpa.repository.Query("SELECT c.city, COUNT(c) FROM Customer c WHERE c.deletedAt IS NULL AND c.city IS NOT NULL AND TRIM(c.city) <> '' GROUP BY c.city ORDER BY COUNT(c) DESC")
    java.util.List<Object[]> findTopLocations(org.springframework.data.domain.Pageable pageable);
}
