package com.crm.platform.delivery.repository;

import com.crm.platform.delivery.entity.CampaignDeliveryOutbox;
import com.crm.platform.delivery.entity.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface CampaignDeliveryOutboxRepository extends JpaRepository<CampaignDeliveryOutbox, Long> {

    List<CampaignDeliveryOutbox> findByStatusOrderByCreatedAtAsc(OutboxStatus status, Pageable pageable);

    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    @Modifying
    @Query("UPDATE CampaignDeliveryOutbox o SET o.status = :newStatus, o.publishedAt = :publishedAt, o.updatedAt = :now WHERE o.id = :id AND o.status = :expectedStatus")
    int markPublished(
            @Param("id") Long id,
            @Param("expectedStatus") OutboxStatus expectedStatus,
            @Param("newStatus") OutboxStatus newStatus,
            @Param("publishedAt") Instant publishedAt,
            @Param("now") Instant now
    );

    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    @Modifying
    @Query("UPDATE CampaignDeliveryOutbox o SET o.retryCount = o.retryCount + 1, o.lastError = :lastError, o.updatedAt = :now WHERE o.id = :id")
    int recordFailure(
            @Param("id") Long id,
            @Param("lastError") String lastError,
            @Param("now") Instant now
    );

    long countByStatus(OutboxStatus status);
}
