package com.crm.platform.ai.repository;

import com.crm.platform.ai.entity.AiSegmentAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiSegmentAuditRepository extends JpaRepository<AiSegmentAudit, Long> {

    @EntityGraph(attributePaths = {"user", "segment"})
    Page<AiSegmentAudit> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
