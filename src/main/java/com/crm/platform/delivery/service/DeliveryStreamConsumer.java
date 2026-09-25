package com.crm.platform.delivery.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

@Component
public class DeliveryStreamConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeliveryStreamConsumer.class);

    private final StringRedisTemplate redisTemplate;
    private final RedisConnectionFactory connectionFactory;
    private final DeliveryWorkerService deliveryWorkerService;

    private final String streamKey;
    private final String consumerGroup;
    private final String consumerName;

    private StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;

    public DeliveryStreamConsumer(
            StringRedisTemplate redisTemplate,
            RedisConnectionFactory connectionFactory,
            DeliveryWorkerService deliveryWorkerService,
            @Value("${crm.async.stream-key:crm:campaign:deliveries:stream}") String streamKey,
            @Value("${crm.async.consumer-group:crm:delivery:workers}") String consumerGroup,
            @Value("${crm.async.consumer-name:worker-1}") String consumerName) {
        this.redisTemplate = redisTemplate;
        this.connectionFactory = connectionFactory;
        this.deliveryWorkerService = deliveryWorkerService;
        this.streamKey = streamKey;
        this.consumerGroup = consumerGroup;
        this.consumerName = consumerName;
    }

    @PostConstruct
    public void start() {
        try {
            ensureConsumerGroupExists();

            StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                    StreamMessageListenerContainerOptions.builder()
                            .pollTimeout(Duration.ofSeconds(1))
                            .executor(Executors.newFixedThreadPool(4))
                            .build();

            container = StreamMessageListenerContainer.create(connectionFactory, options);

            container.receive(
                    Consumer.from(consumerGroup, consumerName),
                    StreamOffset.create(streamKey, ReadOffset.lastConsumed()),
                    message -> {
                        Map<String, String> value = message.getValue();
                        String correlationId = value.get("correlationId");
                        try {
                            if (correlationId != null && !correlationId.isBlank()) {
                                org.slf4j.MDC.put("requestId", correlationId);
                            }
                            String campIdStr = value.get("campaignId");
                            String custIdStr = value.get("customerId");
                            if (campIdStr != null && custIdStr != null) {
                                Long campaignId = Long.parseLong(campIdStr);
                                Long customerId = Long.parseLong(custIdStr);
                                boolean processed = deliveryWorkerService.processDelivery(campaignId, customerId);
                                if (processed) {
                                    redisTemplate.opsForStream().acknowledge(streamKey, consumerGroup, message.getId());
                                }
                            }
                        } catch (Exception e) {
                            log.error("Failed to process delivery stream message id={}", message.getId(), e);
                        } finally {
                            org.slf4j.MDC.remove("requestId");
                        }
                    }
            );

            container.start();
            log.info("Redis Stream consumer started for stream={}, group={}, consumer={}",
                    streamKey, consumerGroup, consumerName);
        } catch (Exception e) {
            log.warn("Could not initialize Redis Stream consumer: {}. Background stream polling disabled.", e.getMessage());
        }
    }

    private void ensureConsumerGroupExists() {
        try {
            redisTemplate.opsForStream().createGroup(streamKey, ReadOffset.from("0"), consumerGroup);
            log.info("Created consumer group {} on stream {}", consumerGroup, streamKey);
        } catch (Exception e) {
            // Group may already exist (BUSYGROUP) or stream not yet initialized
            log.debug("Consumer group {} already exists or could not be created: {}", consumerGroup, e.getMessage());
        }
    }

    @Scheduled(fixedDelayString = "${crm.async.pel-recovery-interval-ms:15000}")
    public int recoverStalePendingMessages() {
        try {
            org.springframework.data.redis.connection.stream.PendingMessages pendingMessages =
                    redisTemplate.opsForStream().pending(
                            streamKey,
                            consumerGroup,
                            org.springframework.data.domain.Range.unbounded(),
                            50
                    );

            if (pendingMessages == null || pendingMessages.isEmpty()) {
                return 0;
            }

            long staleThresholdMs = 30000; // 30 seconds
            int recoveredCount = 0;

            for (org.springframework.data.redis.connection.stream.PendingMessage pm : pendingMessages) {
                if (pm.getElapsedTimeSinceLastDelivery() != null &&
                        pm.getElapsedTimeSinceLastDelivery().toMillis() >= staleThresholdMs) {
                    
                    // Reclaim ownership to this active consumer via XCLAIM
                    List<MapRecord<String, Object, Object>> claimedRecords = redisTemplate.opsForStream().claim(
                            streamKey,
                            consumerGroup,
                            consumerName,
                            Duration.ofMillis(staleThresholdMs),
                            pm.getId()
                    );

                    if (claimedRecords != null && !claimedRecords.isEmpty()) {
                        MapRecord<String, Object, Object> record = claimedRecords.get(0);
                        Map<Object, Object> value = record.getValue();
                        String campIdStr = value.get("campaignId") != null ? value.get("campaignId").toString() : null;
                        String custIdStr = value.get("customerId") != null ? value.get("customerId").toString() : null;
                        String correlationId = value.get("correlationId") != null ? value.get("correlationId").toString() : null;

                        if (campIdStr != null && custIdStr != null) {
                            try {
                                if (correlationId != null && !correlationId.isBlank()) {
                                    org.slf4j.MDC.put("requestId", correlationId);
                                }
                                Long campaignId = Long.parseLong(campIdStr);
                                Long customerId = Long.parseLong(custIdStr);
                                boolean processed = deliveryWorkerService.processDelivery(campaignId, customerId);
                                if (processed) {
                                    redisTemplate.opsForStream().acknowledge(streamKey, consumerGroup, pm.getId());
                                    recoveredCount++;
                                } else {
                                    log.warn("Reclaimed pending message id={} processing not finalized; leaving unacknowledged in PEL", pm.getId());
                                }
                            } finally {
                                org.slf4j.MDC.remove("requestId");
                            }
                        }
                    } else {
                        // Check if the message was trimmed or no longer exists in the stream
                        List<?> existing = redisTemplate.opsForStream().range(
                                streamKey,
                                org.springframework.data.domain.Range.closed(pm.getIdAsString(), pm.getIdAsString())
                        );
                        if (existing == null || existing.isEmpty()) {
                            // Message was pruned from stream; acknowledge to release ghost pending entry
                            redisTemplate.opsForStream().acknowledge(streamKey, consumerGroup, pm.getId());
                        }
                    }
                }
            }

            if (recoveredCount > 0) {
                log.info("Recovered and processed {} stale PEL messages from consumer group {}", recoveredCount, consumerGroup);
            }
            return recoveredCount;
        } catch (Exception e) {
            log.debug("PEL recovery check skipped: {}", e.getMessage());
            return 0;
        }
    }

    @PreDestroy
    public void stop() {
        if (container != null && container.isRunning()) {
            try {
                container.stop();
                log.info("Redis Stream consumer stopped gracefully");
            } catch (Exception e) {
                log.warn("Error stopping Redis Stream container: {}", e.getMessage());
            }
        }
    }
}
