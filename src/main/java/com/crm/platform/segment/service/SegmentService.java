package com.crm.platform.segment.service;

import com.crm.platform.customer.dto.CustomerResponseDto;
import com.crm.platform.customer.entity.Customer;
import com.crm.platform.segment.dto.SegmentCreateRequest;
import com.crm.platform.segment.dto.SegmentPreviewResponse;
import com.crm.platform.segment.dto.SegmentUpdateRequest;
import com.crm.platform.segment.entity.Segment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface SegmentService {

    Segment createSegment(SegmentCreateRequest request, String callerUsername);

    Segment getSegmentById(Long id);

    Segment updateSegment(Long id, SegmentUpdateRequest request);

    void deleteSegment(Long id);

    Page<Segment> listSegments(Pageable pageable);

    SegmentPreviewResponse previewSegment(Long id);

    Page<CustomerResponseDto> getSegmentMembers(Long id, Pageable pageable);

    Specification<Customer> compileSegmentRules(Long id);
}

