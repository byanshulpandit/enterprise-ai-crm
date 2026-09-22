package com.crm.platform.segment.repository;

import com.crm.platform.segment.entity.Segment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SegmentRepository extends JpaRepository<Segment, Long> {

    @Override
    @EntityGraph(attributePaths = {"createdBy"})
    Optional<Segment> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"createdBy"})
    Page<Segment> findAll(Pageable pageable);

    Optional<Segment> findByName(String name);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
