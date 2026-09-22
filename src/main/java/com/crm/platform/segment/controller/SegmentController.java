package com.crm.platform.segment.controller;

import com.crm.platform.common.dto.ApiResponse;
import com.crm.platform.common.dto.PageMetadata;
import com.crm.platform.segment.dto.SegmentCreateRequest;
import com.crm.platform.segment.dto.SegmentResponse;
import com.crm.platform.segment.dto.SegmentUpdateRequest;
import com.crm.platform.segment.entity.Segment;
import com.crm.platform.segment.service.SegmentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/segments")
public class SegmentController {

    private final SegmentService segmentService;

    public SegmentController(SegmentService segmentService) {
        this.segmentService = segmentService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SegmentResponse>> createSegment(
            @Valid @RequestBody SegmentCreateRequest request,
            Authentication authentication) {
        String callerUsername = authentication != null ? authentication.getName() : null;
        Segment created = segmentService.createSegment(request, callerUsername);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(ApiResponse.success(SegmentResponse.fromEntity(created)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SegmentResponse>> getSegmentById(@PathVariable Long id) {
        Segment segment = segmentService.getSegmentById(id);
        return ResponseEntity.ok(ApiResponse.success(SegmentResponse.fromEntity(segment)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<SegmentResponse>> updateSegment(
            @PathVariable Long id,
            @Valid @RequestBody SegmentUpdateRequest request) {
        Segment updated = segmentService.updateSegment(id, request);
        return ResponseEntity.ok(ApiResponse.success(SegmentResponse.fromEntity(updated)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteSegment(@PathVariable Long id) {
        segmentService.deleteSegment(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SegmentResponse>>> listSegments(
            @PageableDefault(page = 0, size = 10, sort = "id") Pageable pageable) {
        Page<Segment> page = segmentService.listSegments(pageable);
        List<SegmentResponse> dtos = page.getContent().stream().map(SegmentResponse::fromEntity).toList();
        PageMetadata pagination = PageMetadata.fromPage(page);
        return ResponseEntity.ok(ApiResponse.success(dtos, pagination));
    }
}
