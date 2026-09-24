package com.crm.platform.delivery.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisPelRecoveryTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private RedisConnectionFactory connectionFactory;

    @Mock
    private DeliveryWorkerService deliveryWorkerService;

    @Mock
    private StreamOperations<String, Object, Object> streamOperations;

    private DeliveryStreamConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new DeliveryStreamConsumer(
                redisTemplate,
                connectionFactory,
                deliveryWorkerService,
                "crm:campaign:deliveries:stream",
                "crm:delivery:workers",
                "worker-test"
        );
    }

    @Test
    @DisplayName("recoverStalePendingMessages reclaims ownership via XCLAIM and acknowledges only after successful delivery persistence")
    void testRecoverStalePendingMessages_ClaimOwnershipSuccess() {
        RecordId recordId = RecordId.of("1700000000000-0");
        PendingMessage pendingMessage = new PendingMessage(
                recordId,
                org.springframework.data.redis.connection.stream.Consumer.from("crm:delivery:workers", "worker-dead"),
                Duration.ofMillis(45000), // 45 seconds idle (> 30s threshold)
                2
        );

        PendingMessages pendingMessages = new PendingMessages(
                "crm:delivery:workers",
                List.of(pendingMessage)
        );

        when(redisTemplate.opsForStream()).thenReturn((StreamOperations) streamOperations);
        when(streamOperations.pending(eq("crm:campaign:deliveries:stream"), eq("crm:delivery:workers"), any(Range.class), eq(50L)))
                .thenReturn(pendingMessages);

        MapRecord<String, Object, Object> streamRecord = MapRecord.create(
                "crm:campaign:deliveries:stream",
                Map.<Object, Object>of("campaignId", "5", "customerId", "50", "correlationId", "corr-999")
        ).withId(recordId);

        when(streamOperations.claim(
                eq("crm:campaign:deliveries:stream"),
                eq("crm:delivery:workers"),
                eq("worker-test"),
                eq(Duration.ofMillis(30000)),
                eq(recordId)
        )).thenReturn(List.of(streamRecord));

        when(deliveryWorkerService.processDelivery(5L, 50L)).thenReturn(true);

        int recovered = consumer.recoverStalePendingMessages();

        assertThat(recovered).isEqualTo(1);
        verify(streamOperations).claim(
                eq("crm:campaign:deliveries:stream"),
                eq("crm:delivery:workers"),
                eq("worker-test"),
                eq(Duration.ofMillis(30000)),
                eq(recordId)
        );
        verify(deliveryWorkerService).processDelivery(5L, 50L);
        verify(streamOperations).acknowledge(eq("crm:campaign:deliveries:stream"), eq("crm:delivery:workers"), eq(recordId));
    }

    @Test
    @DisplayName("recoverStalePendingMessages reclaims ownership but does NOT acknowledge if delivery persistence fails")
    void testRecoverStalePendingMessages_ClaimOwnershipFailure_NoAck() {
        RecordId recordId = RecordId.of("1700000000001-0");
        PendingMessage pendingMessage = new PendingMessage(
                recordId,
                org.springframework.data.redis.connection.stream.Consumer.from("crm:delivery:workers", "worker-crashed"),
                Duration.ofMillis(50000),
                1
        );

        PendingMessages pendingMessages = new PendingMessages(
                "crm:delivery:workers",
                List.of(pendingMessage)
        );

        when(redisTemplate.opsForStream()).thenReturn((StreamOperations) streamOperations);
        when(streamOperations.pending(eq("crm:campaign:deliveries:stream"), eq("crm:delivery:workers"), any(Range.class), eq(50L)))
                .thenReturn(pendingMessages);

        MapRecord<String, Object, Object> streamRecord = MapRecord.create(
                "crm:campaign:deliveries:stream",
                Map.<Object, Object>of("campaignId", "6", "customerId", "60", "correlationId", "corr-fail")
        ).withId(recordId);

        when(streamOperations.claim(
                eq("crm:campaign:deliveries:stream"),
                eq("crm:delivery:workers"),
                eq("worker-test"),
                eq(Duration.ofMillis(30000)),
                eq(recordId)
        )).thenReturn(List.of(streamRecord));

        when(deliveryWorkerService.processDelivery(6L, 60L)).thenReturn(false);

        int recovered = consumer.recoverStalePendingMessages();

        assertThat(recovered).isEqualTo(0);
        verify(deliveryWorkerService).processDelivery(6L, 60L);
        // CRITICAL INVARIANT: Never ACK before terminal MySQL state is successfully persisted
        verify(streamOperations, never()).acknowledge(eq("crm:campaign:deliveries:stream"), eq("crm:delivery:workers"), eq(recordId));
    }

    @Test
    @DisplayName("recoverStalePendingMessages handles claim returning empty when message is already claimed concurrently")
    void testRecoverStalePendingMessages_AlreadyClaimedConcurrently() {
        RecordId recordId = RecordId.of("1700000000002-0");
        PendingMessage pendingMessage = new PendingMessage(
                recordId,
                org.springframework.data.redis.connection.stream.Consumer.from("crm:delivery:workers", "worker-active"),
                Duration.ofMillis(35000),
                1
        );

        when(redisTemplate.opsForStream()).thenReturn((StreamOperations) streamOperations);
        when(streamOperations.pending(eq("crm:campaign:deliveries:stream"), eq("crm:delivery:workers"), any(Range.class), eq(50L)))
                .thenReturn(new PendingMessages("crm:delivery:workers", List.of(pendingMessage)));

        // Another consumer claimed it in the meantime, so claim returns empty
        when(streamOperations.claim(
                eq("crm:campaign:deliveries:stream"),
                eq("crm:delivery:workers"),
                eq("worker-test"),
                eq(Duration.ofMillis(30000)),
                eq(recordId)
        )).thenReturn(List.of());

        // Message still exists in stream (being processed by other consumer)
        MapRecord<String, Object, Object> existingRecord = MapRecord.create(
                "crm:campaign:deliveries:stream",
                Map.<Object, Object>of("campaignId", "7", "customerId", "70")
        ).withId(recordId);
        when(streamOperations.range(eq("crm:campaign:deliveries:stream"), any(Range.class)))
                .thenReturn((List) List.of(existingRecord));

        int recovered = consumer.recoverStalePendingMessages();

        assertThat(recovered).isEqualTo(0);
        verify(deliveryWorkerService, never()).processDelivery(any(), any());
        verify(streamOperations, never()).acknowledge(any(), any(), any(RecordId.class));
    }

    @Test
    @DisplayName("recoverStalePendingMessages acknowledges orphaned pending entry when message was pruned from stream")
    void testRecoverStalePendingMessages_PrunedFromStream_Acknowledged() {
        RecordId recordId = RecordId.of("1700000000003-0");
        PendingMessage pendingMessage = new PendingMessage(
                recordId,
                org.springframework.data.redis.connection.stream.Consumer.from("crm:delivery:workers", "worker-dead"),
                Duration.ofMillis(60000),
                3
        );

        when(redisTemplate.opsForStream()).thenReturn((StreamOperations) streamOperations);
        when(streamOperations.pending(eq("crm:campaign:deliveries:stream"), eq("crm:delivery:workers"), any(Range.class), eq(50L)))
                .thenReturn(new PendingMessages("crm:delivery:workers", List.of(pendingMessage)));

        when(streamOperations.claim(
                eq("crm:campaign:deliveries:stream"),
                eq("crm:delivery:workers"),
                eq("worker-test"),
                eq(Duration.ofMillis(30000)),
                eq(recordId)
        )).thenReturn(List.of());

        when(streamOperations.range(eq("crm:campaign:deliveries:stream"), any(Range.class)))
                .thenReturn((List) List.of());

        int recovered = consumer.recoverStalePendingMessages();

        assertThat(recovered).isEqualTo(0);
        verify(deliveryWorkerService, never()).processDelivery(any(), any());
        verify(streamOperations).acknowledge(eq("crm:campaign:deliveries:stream"), eq("crm:delivery:workers"), eq(recordId));
    }

    @Test
    @DisplayName("recoverStalePendingMessages returns 0 when no pending messages are found")
    void testRecoverStalePendingMessages_NonePending() {
        when(redisTemplate.opsForStream()).thenReturn((StreamOperations) streamOperations);
        when(streamOperations.pending(eq("crm:campaign:deliveries:stream"), eq("crm:delivery:workers"), any(Range.class), eq(50L)))
                .thenReturn(new PendingMessages("crm:delivery:workers", List.of()));

        int recovered = consumer.recoverStalePendingMessages();

        assertThat(recovered).isEqualTo(0);
        verify(deliveryWorkerService, never()).processDelivery(any(), any());
    }
}
