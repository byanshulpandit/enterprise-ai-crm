package com.crm.platform.delivery.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RedisDeliveryStreamProducer implements DeliveryStreamProducer {

    private static final Logger log = LoggerFactory.getLogger(RedisDeliveryStreamProducer.class);

    private final StringRedisTemplate redisTemplate;
    private final String streamKey;

    public RedisDeliveryStreamProducer(
            StringRedisTemplate redisTemplate,
            @Value("${crm.async.stream-key:crm:campaign:deliveries:stream}") String streamKey) {
        this.redisTemplate = redisTemplate;
        this.streamKey = streamKey;
    }

    @Override
    public void enqueueDelivery(Long campaignId, Long customerId) {
        Map<String, String> payload = Map.of(
                "campaignId", String.valueOf(campaignId),
                "customerId", String.valueOf(customerId)
        );
        MapRecord<String, String, String> record = MapRecord.create(streamKey, payload);
        RecordId recordId = redisTemplate.opsForStream().add(record);
        log.debug("Enqueued delivery task to Redis Stream: campaignId={}, customerId={}, recordId={}",
                campaignId, customerId, recordId);
    }

    @Override
    public void enqueueDeliveries(Long campaignId, List<Long> customerIds) {
        if (customerIds == null || customerIds.isEmpty()) {
            return;
        }
        for (Long customerId : customerIds) {
            enqueueDelivery(campaignId, customerId);
        }
        log.info("Successfully enqueued {} delivery tasks for campaignId={}", customerIds.size(), campaignId);
    }
}
