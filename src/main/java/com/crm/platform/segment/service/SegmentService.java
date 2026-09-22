package com.crm.platform.segment.service;

import com.crm.platform.segment.dto.SegmentCreateRequest;
import com.crm.platform.segment.dto.SegmentUpdateRequest;
import com.crm.platform.segment.entity.Segment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SegmentService {

    Segment createSegment(SegmentCreateRequest request, String callerUsername);

    Segment getSegmentById(Long id);

    Segment updateSegment(Long id, SegmentUpdateRequest request);

    void deleteSegment(Long id);

    Page<Segment> listSegments(Pageable pageable);
}
