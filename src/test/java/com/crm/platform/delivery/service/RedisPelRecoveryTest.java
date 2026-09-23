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
    @DisplayName("recoverStalePendingMessages processes messages older than stale threshold and acknowledges them")
    void testRecoverStalePendingMessages_Success() {
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

        MapRecord<String, String, String> streamRecord = MapRecord.create(
                "crm:campaign:deliveries:stream",
                Map.of("campaignId", "5", "customerId", "50", "correlationId", "corr-999")
        ).withId(recordId);

        when(streamOperations.range(eq("crm:campaign:deliveries:stream"), any(Range.class)))
                .thenReturn((List) List.of(streamRecord));

        when(deliveryWorkerService.processDelivery(5L, 50L)).thenReturn(true);

        int recovered = consumer.recoverStalePendingMessages();

        assertThat(recovered).isEqualTo(1);
        verify(deliveryWorkerService).processDelivery(5L, 50L);
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
