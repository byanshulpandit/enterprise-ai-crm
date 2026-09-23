package com.crm.platform.delivery.controller;

import com.crm.platform.common.dto.ApiResponse;
import com.crm.platform.common.dto.PageMetadata;
import com.crm.platform.delivery.dto.DeliveryRecordResponse;
import com.crm.platform.delivery.dto.DeliverySummaryResponse;
import com.crm.platform.delivery.entity.DeliveryStatus;
import com.crm.platform.delivery.service.DeliveryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/campaigns")
public class DeliveryController {

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @GetMapping("/{id}/delivery-summary")
    public ResponseEntity<ApiResponse<DeliverySummaryResponse>> getDeliverySummary(@PathVariable Long id) {
        DeliverySummaryResponse summary = deliveryService.getDeliverySummary(id);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/{id}/deliveries")
    public ResponseEntity<ApiResponse<List<DeliveryRecordResponse>>> getDeliveries(
            @PathVariable Long id,
            @RequestParam(required = false) DeliveryStatus status,
            @PageableDefault(page = 0, size = 20, sort = "id") Pageable pageable) {
        Page<DeliveryRecordResponse> page = deliveryService.getDeliveries(id, status, pageable);
        List<DeliveryRecordResponse> content = page.getContent();
        PageMetadata pagination = PageMetadata.fromPage(page);
        return ResponseEntity.ok(ApiResponse.success(content, pagination));
    }
}
