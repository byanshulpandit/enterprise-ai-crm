package com.crm.platform.delivery.service;

import com.crm.platform.campaign.entity.CampaignStatus;
import com.crm.platform.campaign.repository.CampaignRepository;
import com.crm.platform.delivery.entity.CampaignDeliveryRecord;
import com.crm.platform.delivery.entity.DeliveryStatus;
import com.crm.platform.delivery.repository.CampaignDeliveryRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class DeliveryWorkerServiceImpl implements DeliveryWorkerService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryWorkerServiceImpl.class);

    private final CampaignDeliveryRecordRepository deliveryRecordRepository;
    private final CampaignRepository campaignRepository;
    private final com.crm.platform.delivery.provider.DeliveryProvider deliveryProvider;

    public DeliveryWorkerServiceImpl(
            CampaignDeliveryRecordRepository deliveryRecordRepository,
            CampaignRepository campaignRepository,
            com.crm.platform.delivery.provider.DeliveryProvider deliveryProvider) {
        this.deliveryRecordRepository = deliveryRecordRepository;
        this.campaignRepository = campaignRepository;
        this.deliveryProvider = deliveryProvider;
    }

    @Override
    @Transactional
    public boolean processDelivery(Long campaignId, Long customerId) {
        MDC.put("campaignId", String.valueOf(campaignId));
        MDC.put("customerId", String.valueOf(customerId));
        try {
            Optional<CampaignDeliveryRecord> opt = deliveryRecordRepository.findByCampaignIdAndCustomerId(campaignId, customerId);
            if (opt.isEmpty()) {
                log.warn("Delivery task dropped: No persistent obligation found for campaignId={}, customerId={}",
                        campaignId, customerId);
                return false;
            }

            CampaignDeliveryRecord record = opt.get();
            if (record.getStatus() != DeliveryStatus.PENDING) {
                log.debug("Delivery task already resolved (idempotent skip): campaignId={}, customerId={}, status={}",
                        campaignId, customerId, record.getStatus());
                return true;
            }

            String idempotencyKey = "CAMP-" + campaignId + "-CUST-" + customerId;
            String recipientEmail = (record.getCustomer() != null) ? record.getCustomer().getEmail() : "customer-" + customerId + "@example.com";
            com.crm.platform.delivery.provider.DeliveryRequest request = new com.crm.platform.delivery.provider.DeliveryRequest(
                    idempotencyKey,
                    campaignId,
                    customerId,
                    recipientEmail,
                    record.getMessage()
            );

            com.crm.platform.delivery.provider.DeliveryResult result = deliveryProvider.send(request);
            boolean success = (result != null && result.isSuccess());
            DeliveryStatus newStatus = success ? DeliveryStatus.SENT : DeliveryStatus.FAILED;
            String failureReason = success ? null : (result != null ? result.getFailureReason() : "Delivery provider returned null result");
            Instant processedAt = (result != null && result.getProcessedAt() != null) ? result.getProcessedAt() : Instant.now();

            int rowsUpdated = deliveryRecordRepository.updateStatusIfPending(
                    campaignId,
                    customerId,
                    newStatus,
                    failureReason,
                    processedAt
            );

            if (rowsUpdated > 0) {
                log.info("Delivery finalized: campaignId={}, customerId={}, status={}",
                        campaignId, customerId, newStatus);
            } else {
                log.debug("Concurrent delivery update detected: campaignId={}, customerId={}",
                        campaignId, customerId);
            }

            // Check if all delivery obligations for this campaign have reached terminal states
            checkAndCompleteCampaign(campaignId);

            return true;
        } catch (Exception e) {
            log.error("Failed to process delivery task: campaignId={}, customerId={}", campaignId, customerId, e);
            return false;
        } finally {
            MDC.remove("campaignId");
            MDC.remove("customerId");
        }
    }

    private void checkAndCompleteCampaign(Long campaignId) {
        long pendingRemaining = deliveryRecordRepository.countByCampaignIdAndStatus(campaignId, DeliveryStatus.PENDING);
        if (pendingRemaining == 0) {
            campaignRepository.findById(campaignId).ifPresent(campaign -> {
                if (campaign.getStatus() == CampaignStatus.RUNNING) {
                    campaign.setStatus(CampaignStatus.COMPLETED);
                    campaign.setCompletedAt(Instant.now());
                    campaignRepository.save(campaign);
                    log.info("Campaign {} transitioned to COMPLETED. All deliveries reached terminal status.", campaignId);
                }
            });
        }
    }
}
