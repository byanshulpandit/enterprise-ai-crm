package com.crm.platform.delivery.provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class SimulatedDeliveryProviderTest {

    private SimulatedDeliveryProvider provider;

    @BeforeEach
    void setUp() {
        provider = new SimulatedDeliveryProvider();
    }

    @Test
    @DisplayName("Should send delivery and return result with messageId or failure reason")
    void testSendDelivery_Basic() {
        DeliveryRequest request = new DeliveryRequest(
                "KEY-1",
                1L,
                10L,
                "test@example.com",
                "Hello message"
        );

        DeliveryResult result = provider.send(request);

        assertThat(result).isNotNull();
        assertThat(result.getProcessedAt()).isNotNull();
        assertThat(provider.hasProcessed("KEY-1")).isTrue();
    }

    @Test
    @DisplayName("Should demonstrate idempotency: duplicate send with same key returns identical cached result")
    void testSendDelivery_IdempotentDuplicate() {
        DeliveryRequest request1 = new DeliveryRequest(
                "KEY-IDEMP-123",
                2L,
                20L,
                "user2@example.com",
                "Important message"
        );

        DeliveryResult result1 = provider.send(request1);
        DeliveryResult result2 = provider.send(request1);

        assertThat(result1).isSameAs(result2);
        assertThat(result1.getMessageId()).isEqualTo(result2.getMessageId());
        assertThat(result1.isSuccess()).isEqualTo(result2.isSuccess());
    }

    @Test
    @DisplayName("Should handle concurrent duplicate sends safely using the same idempotency key")
    void testSendDelivery_ConcurrentDuplicate() throws InterruptedException {
        String key = "CONCURRENT-KEY-456";
        DeliveryRequest request = new DeliveryRequest(
                key,
                3L,
                30L,
                "concurrent@example.com",
                "Concurrent message"
        );

        int threads = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicReference<DeliveryResult> ref1 = new AtomicReference<>();
        AtomicReference<DeliveryResult> ref2 = new AtomicReference<>();

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    DeliveryResult res = provider.send(request);
                    if (!ref1.compareAndSet(null, res)) {
                        ref2.set(res);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertThat(ref1.get()).isNotNull();
        assertThat(ref2.get()).isNotNull();
        assertThat(ref1.get()).isSameAs(ref2.get());
    }
}
