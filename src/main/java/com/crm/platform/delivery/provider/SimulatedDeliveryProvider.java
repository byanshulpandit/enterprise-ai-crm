package com.crm.platform.delivery.provider;

import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class SimulatedDeliveryProvider implements DeliveryProvider {

    private final ConcurrentMap<String, DeliveryResult> processedCache = new ConcurrentHashMap<>();

    @Override
    public DeliveryResult send(DeliveryRequest request) {
        String key = request.getIdempotencyKey();
        if (key == null || key.isBlank()) {
            return simulateSend(request);
        }

        return processedCache.computeIfAbsent(key, k -> simulateSend(request));
    }

    private DeliveryResult simulateSend(DeliveryRequest request) {
        int hash = Math.abs((request.getCampaignId() + ":" + request.getCustomerId()).hashCode());
        boolean isSuccess = (hash % 10) != 0;

        if (isSuccess) {
            String messageId = "SIM-MSG-" + UUID.randomUUID();
            return DeliveryResult.success(messageId);
        } else {
            return DeliveryResult.failure("SIMULATED_CARRIER_TIMEOUT: Downstream network destination unreachable");
        }
    }

    public boolean hasProcessed(String idempotencyKey) {
        return processedCache.containsKey(idempotencyKey);
    }

    public void clearCache() {
        processedCache.clear();
    }
}
