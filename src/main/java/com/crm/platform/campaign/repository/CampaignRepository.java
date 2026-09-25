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
    @org.springframework.lang.NonNull
    @EntityGraph(attributePaths = {"segment", "createdBy"})
    Optional<Campaign> findById(@org.springframework.lang.NonNull Long id);

    @EntityGraph(attributePaths = {"segment", "createdBy"})
    Page<Campaign> findByStatus(CampaignStatus status, Pageable pageable);

    @Override
    @org.springframework.lang.NonNull
    @EntityGraph(attributePaths = {"segment", "createdBy"})
    Page<Campaign> findAll(@org.springframework.lang.NonNull Pageable pageable);

    boolean existsBySegmentId(Long segmentId);

    long countBySegmentId(Long segmentId);

    Optional<Campaign> findByName(String name);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT c FROM Campaign c WHERE c.id = :id")
    Optional<Campaign> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);
}
