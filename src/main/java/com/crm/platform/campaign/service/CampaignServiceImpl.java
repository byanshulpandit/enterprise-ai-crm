package com.crm.platform.campaign.service;

import com.crm.platform.campaign.dto.CampaignCreateRequest;
import com.crm.platform.campaign.dto.CampaignUpdateRequest;
import com.crm.platform.campaign.entity.Campaign;
import com.crm.platform.campaign.entity.CampaignStatus;
import com.crm.platform.campaign.repository.CampaignRepository;
import com.crm.platform.common.exception.ConflictException;
import com.crm.platform.common.exception.InvalidRequestException;
import com.crm.platform.common.exception.ResourceNotFoundException;
import com.crm.platform.segment.entity.Segment;
import com.crm.platform.segment.repository.SegmentRepository;
import com.crm.platform.user.entity.User;
import com.crm.platform.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.crm.platform.campaign.dto.CampaignLaunchResponse;
import com.crm.platform.campaign.util.MessageTemplateRenderer;
import com.crm.platform.customer.entity.Customer;
import com.crm.platform.customer.repository.CustomerRepository;
import com.crm.platform.delivery.entity.CampaignDeliveryRecord;
import com.crm.platform.delivery.repository.CampaignDeliveryRecordRepository;
import com.crm.platform.delivery.service.DeliveryStreamProducer;
import com.crm.platform.segment.service.SegmentService;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class CampaignServiceImpl implements CampaignService {

    private final CampaignRepository campaignRepository;
    private final SegmentRepository segmentRepository;
    private final UserRepository userRepository;
    private final SegmentService segmentService;
    private final CustomerRepository customerRepository;
    private final CampaignDeliveryRecordRepository deliveryRecordRepository;
    private final DeliveryStreamProducer deliveryStreamProducer;
    private final com.crm.platform.delivery.repository.CampaignDeliveryOutboxRepository outboxRepository;
    private final com.crm.platform.delivery.service.DeliveryOutboxPublisher outboxPublisher;

    public CampaignServiceImpl(CampaignRepository campaignRepository,
                               SegmentRepository segmentRepository,
                               UserRepository userRepository,
                               SegmentService segmentService,
                               CustomerRepository customerRepository,
                               CampaignDeliveryRecordRepository deliveryRecordRepository,
                               DeliveryStreamProducer deliveryStreamProducer,
                               com.crm.platform.delivery.repository.CampaignDeliveryOutboxRepository outboxRepository,
                               com.crm.platform.delivery.service.DeliveryOutboxPublisher outboxPublisher) {
        this.campaignRepository = campaignRepository;
        this.segmentRepository = segmentRepository;
        this.userRepository = userRepository;
        this.segmentService = segmentService;
        this.customerRepository = customerRepository;
        this.deliveryRecordRepository = deliveryRecordRepository;
        this.deliveryStreamProducer = deliveryStreamProducer;
        this.outboxRepository = outboxRepository;
        this.outboxPublisher = outboxPublisher;
    }

    @Override
    public Campaign createCampaign(CampaignCreateRequest request, String callerUsername) {
        if (request == null) {
            throw new InvalidRequestException("Campaign creation request must not be null");
        }
        Segment segment = segmentRepository.findById(request.getSegmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Segment not found with id: " + request.getSegmentId()));

        User creator = resolveUser(callerUsername);
        String description = request.getDescription() != null ? request.getDescription().trim() : null;

        Campaign campaign = new Campaign(
                request.getName().trim(),
                description,
                segment,
                request.getMessageTemplate().trim(),
                request.getPersonalizationEnabled(),
                creator
        );

        return campaignRepository.save(campaign);
    }

    @Override
    @Transactional(readOnly = true)
    public Campaign getCampaignById(Long id) {
        if (id == null) {
            throw new ResourceNotFoundException("Campaign id must not be null");
        }
        return campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + id));
    }

    @Override
    public Campaign updateCampaign(Long id, CampaignUpdateRequest request) {
        if (request == null) {
            throw new InvalidRequestException("Campaign update request must not be null");
        }
        Campaign campaign = getCampaignById(id);

        if (campaign.getStatus() != CampaignStatus.DRAFT) {
            throw new ConflictException("Campaign updates are permitted only when status is DRAFT. Current status: " + campaign.getStatus());
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            campaign.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            campaign.setDescription(request.getDescription().trim());
        }
        if (request.getSegmentId() != null) {
            Segment segment = segmentRepository.findById(request.getSegmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Segment not found with id: " + request.getSegmentId()));
            campaign.setSegment(segment);
        }
        if (request.getMessageTemplate() != null && !request.getMessageTemplate().isBlank()) {
            campaign.setMessageTemplate(request.getMessageTemplate().trim());
        }
        if (request.getPersonalizationEnabled() != null) {
            campaign.setPersonalizationEnabled(request.getPersonalizationEnabled());
        }

        return campaignRepository.save(campaign);
    }

    @Override
    public void deleteCampaign(Long id) {
        Campaign campaign = getCampaignById(id);

        if (campaign.getStatus() != CampaignStatus.DRAFT) {
            throw new ConflictException("Campaign deletion is permitted only when status is DRAFT. Current status: " + campaign.getStatus());
        }

        campaignRepository.delete(campaign);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Campaign> listCampaigns(CampaignStatus status, Pageable pageable) {
        if (status != null) {
            return campaignRepository.findByStatus(status, pageable);
        }
        return campaignRepository.findAll(pageable);
    }

    @Override
    public CampaignLaunchResponse launchCampaign(Long id) {
        if (id == null) {
            throw new ResourceNotFoundException("Campaign id must not be null");
        }

        Campaign campaign = campaignRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + id));

        if (campaign.getStatus() != CampaignStatus.DRAFT) {
            throw new InvalidRequestException("Only campaigns in DRAFT status can be launched. Current status: " + campaign.getStatus());
        }

        Specification<Customer> spec = segmentService.compileSegmentRules(campaign.getSegment().getId());
        long totalAudienceCount = customerRepository.count(spec);

        if (totalAudienceCount == 0) {
            throw new InvalidRequestException("Campaign launch rejected: Target segment evaluated to 0 matching active customers. Campaign remains in DRAFT.");
        }

        Instant now = Instant.now();
        campaign.setStatus(CampaignStatus.RUNNING);
        campaign.setStartedAt(now);
        campaignRepository.save(campaign);

        String correlationId = org.slf4j.MDC.get("requestId");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = java.util.UUID.randomUUID().toString();
        }

        // Bounded audience materialization via keyset pagination on indexed Customer ID
        int batchSize = 500;
        Long lastSeenId = 0L;
        int totalMaterialized = 0;

        while (true) {
            final Long currentLastId = lastSeenId;
            Specification<Customer> batchSpec = spec.and((root, query, cb) -> cb.gt(root.get("id"), currentLastId));
            org.springframework.data.domain.Pageable pageable =
                    org.springframework.data.domain.PageRequest.of(0, batchSize, org.springframework.data.domain.Sort.by("id").ascending());
            Page<Customer> customerPage = customerRepository.findAll(batchSpec, pageable);

            if (customerPage.isEmpty()) {
                break;
            }

            List<Customer> customers = customerPage.getContent();
            List<CampaignDeliveryRecord> records = new ArrayList<>(customers.size());
            for (Customer customer : customers) {
                String renderedMessage = MessageTemplateRenderer.render(campaign.getMessageTemplate(), customer);
                records.add(new CampaignDeliveryRecord(campaign, customer, renderedMessage));
            }
            List<CampaignDeliveryRecord> savedRecords = deliveryRecordRepository.saveAll(records);

            // Persist outbox records in the exact same MySQL transaction
            List<com.crm.platform.delivery.entity.CampaignDeliveryOutbox> outboxBatch = new ArrayList<>(savedRecords.size());
            for (CampaignDeliveryRecord saved : savedRecords) {
                outboxBatch.add(new com.crm.platform.delivery.entity.CampaignDeliveryOutbox(
                        campaign.getId(),
                        saved.getCustomer().getId(),
                        saved.getId(),
                        correlationId
                ));
            }
            outboxRepository.saveAll(outboxBatch);

            totalMaterialized += savedRecords.size();
            lastSeenId = customers.get(customers.size() - 1).getId();

            if (customers.size() < batchSize) {
                break;
            }
        }

        // Trigger immediate outbox publication after MySQL commit
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            outboxPublisher.triggerImmediatePublish();
                        }
                    }
            );
        } else {
            outboxPublisher.triggerImmediatePublish();
        }

        return new CampaignLaunchResponse(
                campaign.getId(),
                CampaignStatus.RUNNING,
                totalMaterialized,
                "Campaign transitioned to RUNNING and delivery processing has been initiated.",
                now
        );
    }

    private User resolveUser(String username) {
        if (username == null || username.isBlank()) {
            throw new InvalidRequestException("Authenticated user identity is required");
        }
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));
    }
}
