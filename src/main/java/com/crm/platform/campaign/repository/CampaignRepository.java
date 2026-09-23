package com.crm.platform.campaign.repository;

import com.crm.platform.campaign.entity.Campaign;
import com.crm.platform.campaign.entity.CampaignStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    @Override
    @EntityGraph(attributePaths = {"segment", "createdBy"})
    Optional<Campaign> findById(Long id);

    @EntityGraph(attributePaths = {"segment", "createdBy"})
    Page<Campaign> findByStatus(CampaignStatus status, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"segment", "createdBy"})
    Page<Campaign> findAll(Pageable pageable);

    boolean existsBySegmentId(Long segmentId);

    long countBySegmentId(Long segmentId);

    Optional<Campaign> findByName(String name);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT c FROM Campaign c WHERE c.id = :id")
    Optional<Campaign> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);
}
