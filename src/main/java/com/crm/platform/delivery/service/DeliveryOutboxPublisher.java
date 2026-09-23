package com.crm.platform.delivery.service;

import com.crm.platform.delivery.entity.CampaignDeliveryOutbox;
import com.crm.platform.delivery.entity.OutboxStatus;
import com.crm.platform.delivery.repository.CampaignDeliveryOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DeliveryOutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(DeliveryOutboxPublisher.class);

    private final CampaignDeliveryOutboxRepository outboxRepository;
    private final StringRedisTemplate redisTemplate;
    private final String streamKey;
    private final long maxStreamLength;

    public DeliveryOutboxPublisher(
            CampaignDeliveryOutboxRepository outboxRepository,
            StringRedisTemplate redisTemplate,
            @Value("${crm.async.stream-key:crm:campaign:deliveries:stream}") String streamKey,
            @Value("${crm.async.max-stream-length:10000}") long maxStreamLength) {
        this.outboxRepository = outboxRepository;
        this.redisTemplate = redisTemplate;
        this.streamKey = streamKey;
        this.maxStreamLength = maxStreamLength;
    }

    @Scheduled(fixedDelayString = "${crm.async.outbox-poll-interval-ms:2000}")
    public void scheduledPublish() {
        try {
            publishPendingEvents(100);
        } catch (Exception e) {
            log.debug("Outbox scheduled publish skipped or encountered transient error: {}", e.getMessage());
        }
    }

    public void triggerImmediatePublish() {
        try {
            publishPendingEvents(500);
        } catch (Exception e) {
            log.warn("Immediate outbox publish encountered error: {}", e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public int publishPendingEvents(int batchSize) {
        List<CampaignDeliveryOutbox> pending = outboxRepository.findByStatusOrderByCreatedAtAsc(
                OutboxStatus.PENDING,
                PageRequest.of(0, batchSize)
        );

        if (pending.isEmpty()) {
            return 0;
        }

        int publishedCount = 0;
        for (CampaignDeliveryOutbox entry : pending) {
            boolean published = publishSingleEntry(entry);
            if (published) {
                publishedCount++;
            }
        }

        // Apply stream retention trimming to prevent unbounded growth
        try {
            if (maxStreamLength > 0) {
                redisTemplate.opsForStream().trim(streamKey, maxStreamLength, true);
            }
        } catch (Exception e) {
            log.debug("Stream trimming skipped: {}", e.getMessage());
        }

        return publishedCount;
    }

    @Transactional
    public boolean publishSingleEntry(CampaignDeliveryOutbox entry) {
        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("campaignId", String.valueOf(entry.getCampaignId()));
            payload.put("customerId", String.valueOf(entry.getCustomerId()));
            payload.put("deliveryRecordId", String.valueOf(entry.getDeliveryRecordId()));
            if (entry.getCorrelationId() != null && !entry.getCorrelationId().isBlank()) {
                payload.put("correlationId", entry.getCorrelationId());
            }

            redisTemplate.opsForStream().add(streamKey, payload);

            Instant now = Instant.now();
            outboxRepository.markPublished(
                    entry.getId(),
                    OutboxStatus.PENDING,
                    OutboxStatus.PUBLISHED,
                    now,
                    now
            );
            return true;
        } catch (Exception e) {
            log.warn("Failed to publish outbox event id={} to Redis stream: {}", entry.getId(), e.getMessage());
            outboxRepository.recordFailure(entry.getId(), e.getMessage(), Instant.now());
            return false;
        }
    }
}
