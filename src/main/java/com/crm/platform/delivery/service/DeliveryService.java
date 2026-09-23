package com.crm.platform.delivery.service;

import com.crm.platform.delivery.dto.DeliveryRecordResponse;
import com.crm.platform.delivery.dto.DeliverySummaryResponse;
import com.crm.platform.delivery.entity.DeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DeliveryService {

    DeliverySummaryResponse getDeliverySummary(Long campaignId);

    Page<DeliveryRecordResponse> getDeliveries(Long campaignId, DeliveryStatus status, Pageable pageable);
}
