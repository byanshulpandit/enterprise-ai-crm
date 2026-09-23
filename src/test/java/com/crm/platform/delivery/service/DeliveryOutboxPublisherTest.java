package com.crm.platform.delivery.service;

import com.crm.platform.delivery.entity.CampaignDeliveryOutbox;
import com.crm.platform.delivery.entity.OutboxStatus;
import com.crm.platform.delivery.repository.CampaignDeliveryOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryOutboxPublisherTest {

    @Mock
    private CampaignDeliveryOutboxRepository outboxRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private StreamOperations<String, Object, Object> streamOperations;

    private DeliveryOutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new DeliveryOutboxPublisher(
                outboxRepository,
                redisTemplate,
                "crm:campaign:deliveries:stream",
                10000L
        );
    }

    @Test
    @DisplayName("publishPendingEvents publishes pending records to Redis stream and marks them published")
    void testPublishPendingEvents_Success() {
        CampaignDeliveryOutbox outbox = new CampaignDeliveryOutbox(1L, 10L, 100L, "req-123");
        outbox.setId(5L);

        when(outboxRepository.findByStatusOrderByCreatedAtAsc(eq(OutboxStatus.PENDING), any(PageRequest.class)))
                .thenReturn(List.of(outbox));
        when(redisTemplate.opsForStream()).thenReturn((StreamOperations) streamOperations);

        int published = publisher.publishPendingEvents(50);

        assertThat(published).isEqualTo(1);
        verify(streamOperations).add(eq("crm:campaign:deliveries:stream"), anyMap());
        verify(outboxRepository).markPublished(eq(5L), eq(OutboxStatus.PENDING), eq(OutboxStatus.PUBLISHED), any(), any());
        verify(streamOperations).trim(eq("crm:campaign:deliveries:stream"), eq(10000L), eq(true));
    }

    @Test
    @DisplayName("publishPendingEvents records failure when Redis stream publication throws exception")
    void testPublishPendingEvents_RedisFailure() {
        CampaignDeliveryOutbox outbox = new CampaignDeliveryOutbox(1L, 10L, 100L, "req-123");
        outbox.setId(6L);

        when(outboxRepository.findByStatusOrderByCreatedAtAsc(eq(OutboxStatus.PENDING), any(PageRequest.class)))
                .thenReturn(List.of(outbox));
        when(redisTemplate.opsForStream()).thenReturn((StreamOperations) streamOperations);
        when(streamOperations.add(anyString(), anyMap())).thenThrow(new RuntimeException("Redis connection refused"));

        int published = publisher.publishPendingEvents(50);

        assertThat(published).isEqualTo(0);
        verify(outboxRepository).recordFailure(eq(6L), contains("Redis connection refused"), any());
        verify(outboxRepository, never()).markPublished(anyLong(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("publishPendingEvents returns 0 when no pending outbox records exist")
    void testPublishPendingEvents_Empty() {
        when(outboxRepository.findByStatusOrderByCreatedAtAsc(eq(OutboxStatus.PENDING), any(PageRequest.class)))
                .thenReturn(List.of());

        int published = publisher.publishPendingEvents(50);

        assertThat(published).isEqualTo(0);
        verify(redisTemplate, never()).opsForStream();
    }
}
