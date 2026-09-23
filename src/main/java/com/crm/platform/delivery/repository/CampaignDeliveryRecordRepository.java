package com.crm.platform.delivery.repository;

import com.crm.platform.delivery.entity.CampaignDeliveryRecord;
import com.crm.platform.delivery.entity.DeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface CampaignDeliveryRecordRepository extends JpaRepository<CampaignDeliveryRecord, Long> {

    @EntityGraph(attributePaths = {"customer"})
    Page<CampaignDeliveryRecord> findByCampaignId(Long campaignId, Pageable pageable);

    @EntityGraph(attributePaths = {"customer"})
    Page<CampaignDeliveryRecord> findByCampaignIdAndStatus(Long campaignId, DeliveryStatus status, Pageable pageable);

    long countByCampaignIdAndStatus(Long campaignId, DeliveryStatus status);

    long countByCampaignId(Long campaignId);

    Optional<CampaignDeliveryRecord> findByCampaignIdAndCustomerId(Long campaignId, Long customerId);

    @Modifying
    @Query("UPDATE CampaignDeliveryRecord r SET r.status = :newStatus, r.failureReason = :reason, r.processedAt = :now, r.updatedAt = :now WHERE r.campaign.id = :campaignId AND r.customer.id = :customerId AND r.status = 'PENDING'")
    int updateStatusIfPending(@Param("campaignId") Long campaignId,
                              @Param("customerId") Long customerId,
                              @Param("newStatus") DeliveryStatus newStatus,
                              @Param("reason") String reason,
                              @Param("now") Instant now);
}
