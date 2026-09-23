package com.crm.platform.upload.repository;

import com.crm.platform.upload.entity.UploadHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UploadHistoryRepository extends JpaRepository<UploadHistory, Long> {
    Page<UploadHistory> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
