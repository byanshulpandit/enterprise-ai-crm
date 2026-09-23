package com.crm.platform.delivery.service;

import com.crm.platform.campaign.entity.Campaign;
import com.crm.platform.campaign.entity.CampaignStatus;
import com.crm.platform.campaign.repository.CampaignRepository;
import com.crm.platform.common.exception.ResourceNotFoundException;
import com.crm.platform.delivery.dto.DeliveryRecordResponse;
import com.crm.platform.delivery.dto.DeliverySummaryResponse;
import com.crm.platform.delivery.entity.CampaignDeliveryRecord;
import com.crm.platform.delivery.entity.DeliveryStatus;
import com.crm.platform.delivery.repository.CampaignDeliveryRecordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class DeliveryServiceImpl implements DeliveryService {

    private final CampaignRepository campaignRepository;
    private final CampaignDeliveryRecordRepository deliveryRecordRepository;

    public DeliveryServiceImpl(
            CampaignRepository campaignRepository,
            CampaignDeliveryRecordRepository deliveryRecordRepository) {
        this.campaignRepository = campaignRepository;
        this.deliveryRecordRepository = deliveryRecordRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DeliverySummaryResponse getDeliverySummary(Long campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + campaignId));

        long pendingCount = deliveryRecordRepository.countByCampaignIdAndStatus(campaignId, DeliveryStatus.PENDING);
        long sentCount = deliveryRecordRepository.countByCampaignIdAndStatus(campaignId, DeliveryStatus.SENT);
        long failedCount = deliveryRecordRepository.countByCampaignIdAndStatus(campaignId, DeliveryStatus.FAILED);
        long totalAudienceSize = pendingCount + sentCount + failedCount;

        double completionPercentage = 0.0;
        if (totalAudienceSize > 0) {
            BigDecimal completed = BigDecimal.valueOf(sentCount + failedCount);
            BigDecimal total = BigDecimal.valueOf(totalAudienceSize);
            completionPercentage = completed.multiply(BigDecimal.valueOf(100))
                    .divide(total, 2, RoundingMode.HALF_UP)
                    .doubleValue();
        } else if (campaign.getStatus() == CampaignStatus.COMPLETED) {
            completionPercentage = 100.0;
        }

        boolean isTerminal = (campaign.getStatus() == CampaignStatus.COMPLETED
                || campaign.getStatus() == CampaignStatus.FAILED
                || (totalAudienceSize > 0 && pendingCount == 0));

        return new DeliverySummaryResponse(
                campaign.getId(),
                campaign.getStatus(),
                totalAudienceSize,
                pendingCount,
                sentCount,
                failedCount,
                completionPercentage,
                isTerminal
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DeliveryRecordResponse> getDeliveries(Long campaignId, DeliveryStatus status, Pageable pageable) {
        if (!campaignRepository.existsById(campaignId)) {
            throw new ResourceNotFoundException("Campaign not found with id: " + campaignId);
        }

        Page<CampaignDeliveryRecord> page;
        if (status != null) {
            page = deliveryRecordRepository.findByCampaignIdAndStatus(campaignId, status, pageable);
        } else {
            page = deliveryRecordRepository.findByCampaignId(campaignId, pageable);
        }

        return page.map(DeliveryRecordResponse::fromEntity);
    }
}
