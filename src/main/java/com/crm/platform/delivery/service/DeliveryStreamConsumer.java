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
import org.springframework.stereotype.Component;

import java.time.Duration;
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
                        try {
                            Map<String, String> value = message.getValue();
                            String campIdStr = value.get("campaignId");
                            String custIdStr = value.get("customerId");
                            if (campIdStr != null && custIdStr != null) {
                                Long campaignId = Long.parseLong(campIdStr);
                                Long customerId = Long.parseLong(custIdStr);
                                deliveryWorkerService.processDelivery(campaignId, customerId);
                            }
                            redisTemplate.opsForStream().acknowledge(streamKey, consumerGroup, message.getId());
                        } catch (Exception e) {
                            log.error("Failed to process delivery stream message id={}", message.getId(), e);
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
